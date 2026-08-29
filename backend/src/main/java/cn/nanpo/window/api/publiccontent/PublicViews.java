package cn.nanpo.window.api.publiccontent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PublicViews {

    private PublicViews() {
    }

    public record ContactView(String scene, String name, String phone, String businessHours) {
    }

    public record SiteView(
            long id,
            String name,
            String province,
            String city,
            String county,
            String address,
            String summary,
            String mapKeyword,
            String recommendedSeason,
            ContactView visitorService) {
    }

    public record TravelRouteView(
            long id,
            String kind,
            String title,
            String duration,
            String note,
            List<String> steps,
            String source,
            LocalDateTime verifiedAt,
            LocalDateTime expiresAt) {
    }

    public record AttractionView(
            long id,
            String name,
            String category,
            BigDecimal distanceKm,
            int driveMinutes,
            String summary,
            String coverUrl,
            String mapUrl,
            List<String> highlights) {
    }

    public record TravelStopView(String time, String title, String detail) {
    }

    public record TravelPlanView(
            long id,
            String slug,
            String name,
            String duration,
            String suitableFor,
            String distance,
            String summary,
            List<TravelStopView> stops,
            List<String> tips) {
    }

    public record HomestayView(
            long id,
            String name,
            String type,
            String summary,
            String capacity,
            String price,
            String coverUrl,
            String consultationPhone,
            String externalUrl) {
    }

    public record ExperienceView(
            long id,
            String name,
            String type,
            String season,
            String duration,
            String summary,
            String price,
            String coverUrl,
            String videoUrl,
            String bookingNotes) {
    }

    public record ProductSummaryView(
            long id,
            String name,
            String category,
            String season,
            String summary,
            String coverUrl,
            BigDecimal startingPrice,
            String farmerName,
            long farmerId) {
    }

    public record FarmerView(
            long id,
            String code,
            String name,
            String villageGroup,
            String introduction,
            String certificationStatus) {
    }

    public record SkuView(
            long id,
            String code,
            String specification,
            BigDecimal unitPrice,
            String stockNote) {
    }

    public record FarmRecordView(
            long id,
            String stage,
            LocalDateTime occurredAt,
            String text,
            LocalDateTime reviewedAt,
            LocalDateTime publishedAt) {
    }

    public record ProductDetailView(
            ProductSummaryView product,
            FarmerView farmer,
            List<SkuView> skus,
            List<FarmRecordView> productionRecords) {
    }

    public record FarmerDetailView(FarmerView farmer, List<ProductSummaryView> products) {
    }
}
