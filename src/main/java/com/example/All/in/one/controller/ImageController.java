package com.example.All.in.one.controller;

import com.example.All.in.one.model.ImageItem;
import com.example.All.in.one.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@Slf4j
public class ImageController {

    private final ImageService imageService;



    @Value("${app.pagination.default-page-size:3}")  // Default to 12 if not set
    private int defaultPageSize;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "${app.pagination.default-page-size}") int size,
                        Model model) {
        Page<ImageItem> images = imageService.listImages(page, size);

        model.addAttribute("images", images.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", images.getTotalPages());
        model.addAttribute("totalItems", images.getTotalElements());

        return "index";
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/";
        }

        try {
            ImageItem image = imageService.uploadImage(file);
            redirectAttributes.addFlashAttribute("success", "Image uploaded successfully: " + image.getFilename());
        } catch (IOException e) {
            log.error("Failed to upload image", e);
            redirectAttributes.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
        }

        return "redirect:/";
    }

    @DeleteMapping("/api/images/{key}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable String key) {
        try {
            imageService.deleteImage(key);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Image deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete image", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to delete image: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/images")
    @ResponseBody
    public ResponseEntity<Page<ImageItem>> getImagesJson(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.pagination.default-page-size}") int size) {
        return ResponseEntity.ok(imageService.listImages(page, size));
    }
}