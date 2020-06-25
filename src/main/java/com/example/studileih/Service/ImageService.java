package com.example.studileih.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageService {

    // nach diesem Tutorial erstellt: https://grokonez.com/spring-framework/spring-boot/angular-6-upload-get-multipartfile-spring-boot-example

    // Logger is similar to system.out.println, but you can also see the outprint on a server-log (was useful for running on AWS Cloud)
    Logger log = LoggerFactory.getLogger(this.getClass().getName());

    // basePath is the root folder where you saved the project. imageFolderLocation completes the basePath to the image folder location
    private final String basePath = new File("").getAbsolutePath();
    private final Path imageFolderLocation = Paths.get(basePath + "/src/main/resources/images/users");

    /**
     * The image gets send from the frontend as a Multipartfile. Here we save it.
     * There can't be two images in the image folder with the same name, so we throw an exception if that happens.
     * TODO: Find a better solution here, at least tell the user at the frontend that the upload didn't work.
     * @param file
     */
    public ResponseEntity storeUserImage(MultipartFile file) {
        try {
            createUserImageFolder(); //creates a folder named "user". Only if folder doesn't already exist.
            Files.copy(file.getInputStream(), this.imageFolderLocation.resolve(file.getOriginalFilename()));
        } catch (Exception e) {
            System.out.println("Error at imageService:" + e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("a) image with same name was already uploaded - b) uploaded file was not an image - c) file size > 500KBs");
        }
        return ResponseEntity.status(HttpStatus.OK).body("Dein Foto wurde gespeichert.");
    }

    public Resource loadFile(String filename) {
        try {
            Path file = imageFolderLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("It seems like you deleted the images on the server, without also deleting them in the database ");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("It seems like you deleted the images on the server, without deleting them in the database ");
        }
    }

    // wird noch nicht benutzt
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(imageFolderLocation.toFile());
    }

    public void deleteImage(File file) {
        if(file.delete()) // deletes the file and returns boolean
        {
            System.out.println("Old User profile pic was deleted successfully");
        }
        else
        {
            System.out.println("Failed to delete the old User profile pic");
        }
    }

    public void createUserImageFolder() {
        if(Files.notExists(imageFolderLocation)) {
            try {
                Files.createDirectory(imageFolderLocation);
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize storage!");
            }
        }
    }
}



