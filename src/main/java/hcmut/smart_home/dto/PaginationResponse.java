package hcmut.smart_home.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(title = "PaginationResponse", accessMode = Schema.AccessMode.READ_ONLY)
public class PaginationResponse<T> {
    private T[] data;
    private int page;
    private int limit;
    private long total;
    private boolean hasNextPage;
    private boolean hasPrevPage;

    public PaginationResponse(T[] data, int page, int limit, long total, boolean hasNextPage, boolean hasPrevPage) {
        this.data = data;
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.hasNextPage = hasNextPage;
        this.hasPrevPage = hasPrevPage;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public boolean isHasPrevPage() {
        return hasPrevPage;
    }

    public void setHasPrevPage(boolean hasPrevPage) {
        this.hasPrevPage = hasPrevPage;
    }

    public T[] getData() {
        return data;
    }

    public void setData(T[] data) {
        this.data = data;
    }
}