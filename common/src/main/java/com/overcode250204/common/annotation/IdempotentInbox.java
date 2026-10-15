package com.overcode250204.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotentInbox {
    String eventIdSpel(); // Sử dụng Spring Expression Language (SpEL) để trích xuất eventId từ tham số
    String eventType();
}
