package se.bth.netinventory.equipment.error;

public record ApiError(int status, String error, String message) {
}
