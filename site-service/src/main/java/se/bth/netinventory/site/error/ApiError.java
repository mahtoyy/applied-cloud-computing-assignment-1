package se.bth.netinventory.site.error;

public record ApiError(int status, String error, String message) {
}
