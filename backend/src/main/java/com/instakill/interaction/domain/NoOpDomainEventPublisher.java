package com.instakill.interaction.domain;

public class NoOpDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(Object event) {
        // Intentionally does nothing.
    }
}
