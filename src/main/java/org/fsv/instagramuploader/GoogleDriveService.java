package org.fsv.instagramuploader;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GoogleDriveService {
	private static final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);
	private static final String APPLICATION_NAME = "Instagram Uploader";
	private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String CREDENTIALS_FILE_PATH = "src/main/resources/templates/googleCred.json";
	private static final String TOKENS_DIRECTORY_PATH = "tokens"; // Ordner für gespeicherte Refresh-Tokens
	
	private Drive drive;
	private String targetFolderId;
	
	public GoogleDriveService(String team, String folderName, String dateFolder) {
		try {
			// 1. Load Client Secrets
			File credentialsFile = new File(CREDENTIALS_FILE_PATH);
			if (!credentialsFile.exists()) {
				throw new FileNotFoundException("Credentials-Datei nicht gefunden unter: " + credentialsFile.getAbsolutePath());
			}
			GoogleClientSecrets clientSecrets;
			try (InputStream in = Files.newInputStream(credentialsFile.toPath())) {
				clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in, StandardCharsets.UTF_8));
			}
			
			// 2. Build flow and trigger user authorization if needed
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
							GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets,
							Collections.singletonList(DriveScopes.DRIVE))
							.setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
							.setAccessType("offline")
							.build();
			
			Credential credential = flow.loadCredential("user");
			if (credential == null) {
				logger.error("Kein gespeichertes Token gefunden! Authentifizierung auf dem Server nicht möglich.");
				return;
			}
			
			// 3. Build Drive API
			drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
							.setApplicationName(APPLICATION_NAME)
							.build();
			
			// Ordnerstruktur: Meine Ablage -> Bilder -> Spieltage -> <Team> -> <Gegner> -> <Datum>
			String bilderFolderId = "1mcuySGtiAw6O9bE17xthqeKU_I1jBz6d";
			String spieltageFolderId = getFolder(bilderFolderId, "Spieltage");
			String teamFolderId = getFolder(spieltageFolderId, resolveTeamFolderName(team));
			String opponentFolderId = getFolder(teamFolderId, folderName);
			this.targetFolderId = getFolder(opponentFolderId, dateFolder);
			
		} catch (IOException | GeneralSecurityException e) {
			logger.error("Fehler bei OAuth-Autorisierung", e);
		}
	}
	
	private String resolveTeamFolderName(String team) {
		if (team == null || team.isBlank()) {
			logger.warn("Keine Mannschaft angegeben, verwende '1. Mannschaft' als Standard");
			return "1. Mannschaft";
		}
		String normalized = team.trim();
		if (normalized.matches("\\d+")) {
			return normalized + ". Mannschaft";
		}
		if (normalized.matches("\\d+\\. Mannschaft")) {
			return normalized;
		}
		logger.warn("Unerwarteter Mannschaftswert '{}', verwende '1. Mannschaft' als Standard", team);
		return "1. Mannschaft";
	}
	
	private String createFolder(String folderId, String folderName) {
		com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
		fileMetadata.setName(folderName);
		fileMetadata.setMimeType("application/vnd.google-apps.folder");
		if (!Objects.equals(folderId, "")) {
			fileMetadata.setParents(Collections.singletonList(folderId));
		}
		try {
			com.google.api.services.drive.model.File file = drive.files().create(fileMetadata)
							.setSupportsAllDrives(true)
							.setFields("id")
							.execute();
			logger.info("Created Google Drive folder '{}': id={}", folderName, file.getId());
			return file.getId();
		} catch (IOException e) {
			logger.error("Could not create Google Drive folder '{}' under parent '{}': {}", folderName, folderId, e.getMessage(), e);
		}
		return "";
	}

	private String getFolder(String folderId, String folderName) throws IOException {
		String escapedName = folderName.replace("'", "\\'");
		String setQ = "mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + escapedName + "'";
		if (!Objects.equals(folderId, "")) {
			setQ += " and '" + folderId + "' in parents";
		}
		FileList result = drive.files().list()
						.setQ(setQ)
						.setSupportsAllDrives(true)
						.setIncludeItemsFromAllDrives(true)
						.setPageSize(3)
						.execute();
		List<com.google.api.services.drive.model.File> files = result.getFiles();
		if (files != null && !files.isEmpty()) {
			return files.get(0).getId();
		}
		logger.warn("Google Drive folder '{}' not found under '{}', creating it.", folderName, folderId);
		return createFolder(folderId, folderName);
	}
	
	public void uploadFileToFolder(File file) {
		uploadFileToFolder(file, file.getName());
	}
	
	public void uploadFileToFolder(File file, String driveFileName) {
		if (drive == null) {
			logger.error("Skipping Google Drive upload for '{}': drive service is not initialized (check credentials/OAuth)", driveFileName);
			return;
		}
		if (targetFolderId == null || targetFolderId.isBlank()) {
			logger.error("Skipping Google Drive upload for '{}': target folder is not available (folder creation may have failed)", driveFileName);
			return;
		}
		com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
		fileMetadata.setParents(Collections.singletonList(targetFolderId));
		fileMetadata.setName(driveFileName);
		FileContent mediaContent = new FileContent("image/jpeg", file);
		try {
			com.google.api.services.drive.model.File uploadedFile = drive.files().create(fileMetadata, mediaContent)
							.setFields("id")
							.setSupportsAllDrives(true)
							.execute();
			logger.info("Uploaded image to Google Drive: fileName='{}', id='{}'", driveFileName, uploadedFile.getId());
			
		} catch (IOException e) {
			logger.error("Could not upload image '{}' to Google Drive folder '{}'", driveFileName, targetFolderId, e);
		}
	}
	
	public void uploadImageToFolder(BufferedImage image, String fileName) {
		if (drive == null || targetFolderId == null || targetFolderId.isBlank()) {
			logger.error("Skipping Google Drive upload for '{}': service is unavailable (drive={}, targetFolderId={})", fileName, drive != null, targetFolderId == null ? "null" : (targetFolderId.isBlank() ? "blank" : targetFolderId));
			return;
		}
		File tempFile = null;
		try {
			tempFile = File.createTempFile("drive_upload_", "_" + fileName);
			ImageIO.write(image, "jpeg", tempFile);
			uploadFileToFolder(tempFile, fileName);
		} catch (IOException e) {
			logger.error("Could not write image '{}' to temp file for Google Drive upload", fileName, e);
		} finally {
			if (tempFile != null && !tempFile.delete()) {
				logger.warn("Could not delete temp file '{}'", tempFile.getAbsolutePath());
			}
		}
	}
	/*
	private static String getPathToGoogleCredentials() {
		String configuredPath = System.getenv("GOOGLE_CREDENTIALS_FILE");
		if (configuredPath != null && !configuredPath.isBlank()) {
			return configuredPath;
		}
		String currentDirectory = System.getProperty("user.dir");
		return Paths.get(currentDirectory, "src/main/resources/templates/googleCred.json").toString();
	}
	*/
}