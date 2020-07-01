package com.example.studileih.Controller;

import com.example.studileih.Entity.Product;
import com.example.studileih.Entity.User;
import com.example.studileih.Service.ImageService;
import com.example.studileih.Service.ProductService;
import com.example.studileih.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@CrossOrigin
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    /*
    * loads a product pic
     */
    @PostMapping("/images/loadProductPicByFilename")    //https://bezkoder.com/spring-boot-upload-multiple-files/
    public ResponseEntity loadProductPicByFilename(@RequestParam("filename") String filename, String productId) {
        // load the picture from the local storage
            return imageService.loadImageByFilename(filename, Long.parseLong(productId));  // loadImageByFilename() returns a response with the product pic. If the image couldn't be loaded, the response will contain an error message
    }

    /*
     * method for posting images into the image folder (src -> main -> resources -> images) and put the filePath into the database.
     */
    @PostMapping("/postImage")
    public ResponseEntity handleFileUpload(@RequestParam("file") MultipartFile file, String userId, String productId, String imgType) {
        // -> if the image is a userPic -> update the user who posted it with the newly generated photo filePath of the just saved photo
        if (imgType.equals("userPic")) {
            return saveUserPic(file, userId);
        } else if (imgType.equals("productPic")) {
            return saveProductPic(file, productId);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Der imgType im Angular Code war falsch...");
    }

    private ResponseEntity saveProductPic(MultipartFile file, String productId) {
        Product product = getProduct(productId);                                      // We first load the product, for which we wanna save the pic.
        if (product == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("product with Id" + productId + " doesn't exist in db.");    // Returns a status = 404 response
        if (!hasProductAlreadyHasThisFile(file, product)) {                         // checks, if User is currently using a Photo with exact same name as ProfilePic
            ResponseEntity response = imageService.storeImage(file, "product", product.getId());  //übergibt das Foto zum Speichern an imageService und gibt den Namen des Fotos zum gerade gespeicherten Foto zurück als Response Body. falls Speichern nicht geklappt hat kommt response mit Fehlercode zurück (400 oder ähnliches)
            if (response.getStatusCodeValue() == 200) {
                System.out.println("Unter folgendem Namen wurde das Foto lokal (src -> main -> resources -> images) gespeichert: " + file.getOriginalFilename()); // if saving Pfoto was successfull => response status = 200...
                if (product.getPicPaths() == null) {                                   // we only saved the pic in the pic folder until now, not in the database. A Product can have multiple images, so the pic needs to get stored in a List
                    ArrayList<String> arr = new ArrayList();
                    arr.add(file.getOriginalFilename());
                    product.setPicPaths(arr);
                } else {
                    product.getPicPaths().add(file.getOriginalFilename());
                    product.setPicPaths(product.getPicPaths());
                }
                productService.addProduct(product);                             //updated das Product in der datenbank, damit der Fotoname da auch gespeichert ist.
            }
            return response;
        }
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body("Foto mit selbem Namen wurde für gleiches Produkt schonmal hochgeladen.");
    }

    private ResponseEntity saveUserPic(MultipartFile file, String userId) {
        User user = getActiveUser(userId);                                      // We first load the user, for whom we wann save the profile pic.
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user with Id" + userId + " doesn't exist in db.");    // Returns a status = 404 response
        if (user.getProfilePic() != null) deleteOldProfilePic(user);            // deletes old User Pic, so that there's always only one profile pic
            ResponseEntity response = imageService.storeImage(file, "user", user.getId());  //übergibt das Foto zum Speichern an imageService und gibt den Namen des Fotos zum gerade gespeicherten Foto zurück als Response Body. falls Speichern nicht geklappt hat kommt response mit Fehlercode zurück (400 oder ähnliches)
            if (response.getStatusCodeValue() == 200) {                         // if saving Pfoto was successfull => response status = 200...
                System.out.println("Unter folgendem Namen wurde das Foto lokal (src -> main -> resources -> images) gespeichert: " + file.getOriginalFilename());
                user.setProfilePic(file.getOriginalFilename());
                userService.saveOrUpdateUser(user);                             //updated den User in der datenbank, damit der Fotoname da auch gespeichert ist.
            }
            return response;
    }

    private void deleteOldProfilePic(User user) {
        try {
            Resource resource = imageService.loadUserProfilePic(user.getProfilePic(), user);
            imageService.deleteImage(new File(resource.getURI()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    *
     */
    @PostMapping("/loadProfilePic")
    public ResponseEntity getImageByUserId(@RequestBody String userId) {
        // The file Names of the ProfilePics are saved in the user entities, so we first need to load the user from the user database table
        User user = getActiveUser(userId);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("user with Id" + userId + " doesn't exist in db.");    // Returns a status = 404 response
        // Then we load the picture from the local storage
        if (user.getProfilePic() != null) {
            Resource file = imageService.loadUserProfilePic(user.getProfilePic(), user);  // Somehow you can't store the file directly in a variable of type File, instead you need to use a variable of type Resource.
            System.out.println(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")  //the Content-Disposition response header is a header indicating if the content is expected to be displayed inline in the browser, that is, as a Web page or as part of a Web page, or as an attachment, that is downloaded and saved locally.
                    .body(file);  // the response body now contains the profile pic
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with userId = " + userId + "has no profilePic yet (user.getProfilePic() = null)");  // Returns a status = 404 response
        }

    }

    public User getActiveUser(String userId) {
        try {
            Optional<User> optionalEntity = userService.getUserById(Long.parseLong(userId));
            return optionalEntity.get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public Product getProduct(String productId) {
        try {
            Optional<Product> optionalEntity = productService.getProductById(Long.parseLong(productId));
            return optionalEntity.get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private boolean hasProductAlreadyHasThisFile(MultipartFile file, Product product) {
        if (product.getPicPaths() != null) {
            Path path = Paths.get(file.getOriginalFilename());
            System.out.println("Path: " + path);
            if (product.getPicPaths().contains(path)) return true;
        }
        return false;
    }
}