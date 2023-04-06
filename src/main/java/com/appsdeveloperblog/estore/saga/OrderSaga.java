package com.appsdeveloperblog.estore.saga;

import com.appsdeveloperblog.estore.OrdersService.core.events.OrderCreatedEvent;
import com.appsdeveloperblog.estore.core.commands.ReserveProductCommand;
import com.appsdeveloperblog.estore.core.events.ProductReserveEvent;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

// this annotation will make our class a spring component
@Saga
public class OrderSaga {

    @Autowired
    private transient CommandGateway commandGateway;

    private final Logger LOGGER = LoggerFactory.getLogger(OrderSaga.class);

    // associate the event with our current saga object we use the association property which is a attribute in the
    // orderCreatedEvent object
    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderCreatedEvent orderCreatedEvent) {

        // this event handler will publish a new command which will be processed by products microservice,
        // and if everything is good products microservice will publish an event notifying saga that product has
        // been reserved.

        ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
                .orderId(orderCreatedEvent.getOrderId())
                .quantity(orderCreatedEvent.getQuantity())
                .productId(orderCreatedEvent.getProductId())
                .userId(orderCreatedEvent.getUserId())
                .build();

        LOGGER.info("OrderCreatedEvent handled for orderId: " + reserveProductCommand.getOrderId() +
                " and productId: " + reserveProductCommand.getProductId());

        // now we will use command gateway to send this command to command bus
        // we will also implement one callback method which will be invoked by axon framework when the command
        // we have sent is processed or if there are any errors (in errors case we can raise a compensating
        // transaction event).
        commandGateway.send(reserveProductCommand, new CommandCallback<ReserveProductCommand, Object>() {
            @Override
            public void onResult(CommandMessage<? extends ReserveProductCommand> commandMessage,
                                 CommandResultMessage<?> commandResultMessage) {

                if (commandResultMessage.isExceptional()) {
                    // start compensating transaction if exception occurs
                }
            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(ProductReserveEvent productReserveEvent) {

        // process user payment. Saga will send new command to process payment

        LOGGER.info("ProductReserveEvent handled for orderId: " + productReserveEvent.getOrderId() +
                " and productId: " + productReserveEvent.getProductId());
    }
}
