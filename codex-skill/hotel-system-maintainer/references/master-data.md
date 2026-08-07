# HotelSystem Master Data Notes

Use this reference when changing status/type behavior. Confirm names and IDs in the live database before adding assumptions.

## Known master relationships

- Booking status: `t_booking_status`, referenced by `t_booking.id_booking_status`.
- Payment status: `t_payment_status`, referenced by `t_payment.id_payment_status`.
- Room status: `t_room_status`, referenced by `t_room.id_room_status`.
- Stay type: `t_stay_type`, referenced by booking/guest/transfer stay-type IDs.
- Bill status: existing monthly-rent bill status master/relation.
- Receipt type: `t_reciept_type`.
- Payment item: `t_payment_item`; payment detail: `t_payment_detail`.

## Internal codes versus display names

Internal codes may remain in Java conditions and request values, for example `PAID`, `PENDING`, `MONTHLY`, and `AVAILABLE`. UI output must use the related master `name` or a model label getter. Never replace a code in a query condition merely to make the UI Thai.

## Safe status-label pattern

Prefer:

```java
public String getStatus() { return statusMaster != null ? statusMaster.getCode() : fallbackCode(); }
public String getStatusLabel() { return statusMaster != null ? statusMaster.getName() : fallbackThaiName(); }
```

Templates should use `status` for conditions and `statusLabel` for visible text.
