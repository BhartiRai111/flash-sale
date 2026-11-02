package com.bharti.flashsale.inventory.domain;

public enum ReservationStatus {
    /** Stock was taken for the order. */
    RESERVED,
    /** Stock could not be taken (sold out / unknown product). */
    REJECTED,
    /** Stock was given back because payment failed (saga compensation). */
    RELEASED
}
