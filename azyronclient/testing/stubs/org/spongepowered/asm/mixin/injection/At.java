package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface At {
    String value();
    String target() default "";
    int ordinal() default -1;
}
