package org.fsv.instagramuploader;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	
	public GoogleDriveService(String folderName) {
		try {
			// 1. Load Client Secrets
			File credentialsFile = new File(CREDENTIALS_FILE_PATH);
			if (!credentialsFile.exists()) {
				throw new FileNotFoundException("Credentials-Datei nicht gefunden unter: " + credentialsFile.getAbsolutePath());
			}
			GoogleClientSecrets clientSecrets;
			try (InputStream in = Files.newInputStream(credentialsFile.toPath())) {
				clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
			}
			
			// 2. Build flow and trigger user authorization if needed
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
							GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets,
							Collections.singletonList(DriveScopes.DRIVE_FILE))
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
			
			// Ordnerstruktur navigieren (wie gehabt)
			String bilderFolderId = "1mcuySGtiAw6O9bE17xthqeKU_I1jBz6d";
			String spieltageFolderId = getFolder(bilderFolderId, "Spieltage");
			String teamFolderId = getFolder(spieltageFolderId, "1. Mannschaft");
			this.targetFolderId = getFolder(teamFolderId, folderName);
			
		} catch (Exception e) {
			logger.error("Fehler bei OAuth-Autorisierung", e);
		}
	}
	
	private String createFolder(String folderId, String folderName) throws IOException {
		com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
		fileMetadata.setName(folderName);
		fileMetadata.setParents(Collections.singletonList(folderId));
		fileMetadata.setMimeType("application/vnd.google-apps.folder");
		try {
			com.google.api.services.drive.model.File file = drive.files().create(fileMetadata)
							.setSupportsAllDrives(true)
							.setFields("id")
							.execute();
			logger.info("Created Google Drive folder '{}': id={}", folderName, file.getId());
			return file.getId();
		} catch (GoogleJsonResponseException e) {
			logger.error("Could not create Google Drive folder '{}' under parent '{}': {}", folderName, folderId, e.getDetails(), e);
		}
		return "";
	}
	
	private String getFolder(String folderId, String folderName) throws IOException {
		String setQ = "mimeType='application/vnd.google-apps.folder' and name='" + folderName + "'";
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
		if (files == null || files.isEmpty()) {
			return "";
		} else {
			return (String) files.get(0).get("id");
		}
	}
	
	public void uploadFileToFolder(File file) {
		if (drive == null || targetFolderId == null || targetFolderId.isBlank()) {
			logger.debug("Skipping Google Drive upload because the service is unavailable");
			return;
		}
		com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
		fileMetadata.setParents(Collections.singletonList(targetFolderId));
		fileMetadata.setName(file.getName());
		FileContent mediaContent = new FileContent("image/jpeg", file);
		try {
			com.google.api.services.drive.model.File uploadedFile = drive.files().create(fileMetadata, mediaContent)
							.setFields("id")
							.setSupportsAllDrives(true)
							.execute();
			logger.info("Uploaded image to Google Drive: fileName='{}', id='{}'", file.getName(), uploadedFile.getId());
			
		} catch (IOException e) {
			logger.error("Could not upload image '{}' to Google Drive folder '{}'", file.getName(), targetFolderId, e);
		}
	}
	
	private static String getPathToGoogleCredentials() {
		String configuredPath = System.getenv("GOOGLE_CREDENTIALS_FILE");
		if (configuredPath != null && !configuredPath.isBlank()) {
			return configuredPath;
		}
		String currentDirectory = System.getProperty("user.dir");
		return Paths.get(currentDirectory, "src/main/resources/templates/googleCred.json").toString();
	}
}