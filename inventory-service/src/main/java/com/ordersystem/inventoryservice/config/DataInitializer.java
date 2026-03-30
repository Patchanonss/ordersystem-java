package com.ordersystem.inventoryservice.config;

import com.ordersystem.inventoryservice.model.Product;
import com.ordersystem.inventoryservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            List<Product> products = List.of(
                    Product.builder().name("Laptop").stock(50).updatedAt(LocalDateTime.now()).build(),
                    Product.builder().name("Phone").stock(100).updatedAt(LocalDateTime.now()).build(),
                    Product.builder().name("Tablet").stock(75).updatedAt(LocalDateTime.now()).build(),
                    Product.builder().name("Headphones").stock(200).updatedAt(LocalDateTime.now()).build(),
                    Product.builder().name("Keyboard").stock(150).updatedAt(LocalDateTime.now()).build(),
                    Product.builder().name("Mouse").stock(180).updatedAt(LocalDateTime.now()).build(),
                    Product.builder().name("Monitor").stock(40).updatedAt(LocalDateTime.now()).build(),
                    Product.builder().name("Webcam").stock(90).updatedAt(LocalDateTime.now()).build()
            );
            productRepository.saveAll(products);
            log.info("Initialized {} products in inventory", products.size());
        } else {
            log.info("Products already exist, skipping initialization");
        }
    }
}
