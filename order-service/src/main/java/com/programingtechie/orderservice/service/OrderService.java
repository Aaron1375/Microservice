package com.programingtechie.orderservice.service;

import com.programingtechie.orderservice.dto.InventoryResponse;
import com.programingtechie.orderservice.dto.OrderLineItemsDto;
import com.programingtechie.orderservice.dto.OrderRequest;
import com.programingtechie.orderservice.model.Order;
import com.programingtechie.orderservice.model.OrderLineItems;
import com.programingtechie.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        // Call inventory Service, and place order if producsdasda is in
        // Stock
        InventoryResponse[] inventoryResponsesArr = webClient.get()
                .uri("http://localhost:8083/api/inventory?",
//                .uri("http://localhost:8083/api/inventory?skuCode=iphone_13_red&skuCode=iphone_13")
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponsesArr)
                .allMatch(InventoryResponse::isInStock);

        System.out.println(skuCodes);
        System.out.println(allProductsInStock);

        if (allProductsInStock) {
            orderRepository.save(order);
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItems.getQuantity());
        return orderLineItems;
    }
}
