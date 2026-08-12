package org.fsv.instagramuploader;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GoogleDriveService {
 private static final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);
 private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
 private static final String SERVICE_ACCOUNT_KEY_PATH = getPathToGoogleCredentials();
 
 private Drive drive;
 private String targetFolderId;
 
 public GoogleDriveService(String folderName) {
	Path credentialsPath = Paths.get(SERVICE_ACCOUNT_KEY_PATH);
	if (!Files.isRegularFile(credentialsPath)) {
	 logger.warn("Google credentials not found at '{}'; Drive upload is disabled", credentialsPath);
	 return;
	}
	try {
	 GoogleCredentials credentials;
	 try (InputStream input = Files.newInputStream(credentialsPath)) {
		credentials = GoogleCredentials.fromStream(input)
						.createScoped(Collections.singleton(DriveScopes.DRIVE));
	 }

	 drive = new Drive.Builder(
					 GoogleNetHttpTransport.newTrustedTransport(),
					 JSON_FACTORY,
					 new HttpCredentialsAdapter(credentials))
					 .setApplicationName("FSV-Pictures")
					 .build();
	 
	 String fsvFolderId = getFolder("", "FSV");
	 String teamFolderId = getFolder(fsvFolderId, folderName);
	 if (Objects.equals(teamFolderId, "")) {
		teamFolderId = createFolder(fsvFolderId, folderName);
	 }
	 targetFolderId = teamFolderId;
	} catch (IOException | GeneralSecurityException e) {
	 logger.error("Error creating Google Drive service", e);
	}
 }
 
 private String createFolder(String folderId, String folderName) throws IOException {
	com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
	fileMetadata.setName(folderName);
	fileMetadata.setParents(Collections.singletonList(folderId));
	fileMetadata.setMimeType("application/vnd.google-apps.folder");
	try {
	 com.google.api.services.drive.model.File file = drive.files().create(fileMetadata)
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
					 .setFields("id").execute();
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
