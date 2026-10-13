package com.elogistic.backend.orders.internal.domain;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record DraftInput(
 @NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._/-]{0,79}") String reference,
 @NotBlank @Size(max=500) String pickupAddress,
 @NotBlank @Size(max=160) String recipientName,
 @NotBlank @Pattern(regexp="[+0-9 ()-]{7,30}") String recipientPhone,
 @NotBlank @Size(max=300) String deliveryStreet,
 @NotBlank @Size(max=160) String deliveryWard,
 @NotBlank @Size(max=160) String deliveryProvince,
 @NotBlank @Size(max=300) String parcelDescription,
 @NotNull @DecimalMin("0.001") @DecimalMax("1000") @Digits(integer=4,fraction=3) BigDecimal weightKg
) {
 public DraftInput {
  reference=trim(reference);pickupAddress=trim(pickupAddress);recipientName=trim(recipientName);recipientPhone=trim(recipientPhone);
  deliveryStreet=trim(deliveryStreet);deliveryWard=trim(deliveryWard);deliveryProvince=trim(deliveryProvince);parcelDescription=trim(parcelDescription);
  if(weightKg!=null)weightKg=weightKg.stripTrailingZeros();
 }
 private static String trim(String s){return s==null?null:s.trim();}
}
