package com.portfolio.eventsourcing;

import com.portfolio.eventsourcing.domain.OrderService;
import com.portfolio.eventsourcing.domain.OrderEventRepository;
import com.portfolio.eventsourcing.application.port.out.PaymentAuthorizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EventsourcingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventsourcingApplication.class, args);
    }

    @Bean
    public OrderService orderService(OrderEventRepository repository, PaymentAuthorizer paymentAuthorizer) {
        return new OrderService(repository, paymentAuthorizer);
    }
}
