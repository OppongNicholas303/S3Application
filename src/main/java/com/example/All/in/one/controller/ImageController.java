package com.example.All.in.one.controller;

import com.example.All.in.one.model.ImageItem;
import com.example.All.in.one.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ImageController {

    private final ImageService imageService;
    private static final int PAGE_SIZE = 6; // Number of images per page

    @Autowired
    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        Page<ImageItem> imagesPage = imageService.listImages(page, PAGE_SIZE);

        model.addAttribute("images", imagesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", imagesPage.getTotalPages());
        model.addAttribute("totalItems", imagesPage.getTotalElements());

        return "index";
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/";
            }

            ImageItem uploadedImage = imageService.uploadImage(file);
            redirectAttributes.addFlashAttribute("success", "Image uploaded successfully: " + uploadedImage.getFilename());

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
        }

        return "redirect:/";
    }

    @DeleteMapping("/api/images/{key}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable String key) {
        Map<String, Object> response = new HashMap<>();

        try {
            imageService.deleteImage(key);
            response.put("status", "success");
            response.put("message", "Image deleted successfully");

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}