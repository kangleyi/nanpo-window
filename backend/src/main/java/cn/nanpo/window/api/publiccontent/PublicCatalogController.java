package cn.nanpo.window.api.publiccontent;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.publiccontent.PublicViews.AttractionView;
import cn.nanpo.window.api.publiccontent.PublicViews.ExperienceView;
import cn.nanpo.window.api.publiccontent.PublicViews.FarmerDetailView;
import cn.nanpo.window.api.publiccontent.PublicViews.HomestayView;
import cn.nanpo.window.api.publiccontent.PublicViews.ProductDetailView;
import cn.nanpo.window.api.publiccontent.PublicViews.ProductSummaryView;
import cn.nanpo.window.api.publiccontent.PublicViews.SiteView;
import cn.nanpo.window.api.publiccontent.PublicViews.TravelPlanView;
import cn.nanpo.window.api.publiccontent.PublicViews.TravelRouteView;
import cn.nanpo.window.application.PublicCatalogService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.common.api.PageResponse;

@RestController
@RequestMapping("/api/public")
public class PublicCatalogController {

    private final PublicCatalogService service;

    public PublicCatalogController(PublicCatalogService service) {
        this.service = service;
    }

    @GetMapping("/site")
    public ApiResponse<SiteView> site() {
        return ApiResponse.success(service.site());
    }

    @GetMapping("/travel/routes")
    public ApiResponse<List<TravelRouteView>> travelRoutes() {
        return ApiResponse.success(service.travelRoutes());
    }

    @GetMapping("/attractions")
    public ApiResponse<PageResponse<AttractionView>> attractions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.attractions(page, size));
    }

    @GetMapping("/travel-plans")
    public ApiResponse<List<TravelPlanView>> travelPlans() {
        return ApiResponse.success(service.travelPlans());
    }

    @GetMapping("/homestays")
    public ApiResponse<PageResponse<HomestayView>> homestays(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.homestays(page, size));
    }

    @GetMapping("/experiences")
    public ApiResponse<PageResponse<ExperienceView>> experiences(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.experiences(page, size));
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductSummaryView>> products(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.products(page, size));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDetailView> product(@PathVariable long productId) {
        return ApiResponse.success(service.product(productId));
    }

    @GetMapping("/farmers/{farmerId}")
    public ApiResponse<FarmerDetailView> farmer(@PathVariable long farmerId) {
        return ApiResponse.success(service.farmer(farmerId));
    }
}

