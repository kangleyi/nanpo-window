package cn.nanpo.window.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.api.publiccontent.PublicViews.AttractionView;
import cn.nanpo.window.api.publiccontent.PublicViews.ExperienceView;
import cn.nanpo.window.api.publiccontent.PublicViews.FarmerDetailView;
import cn.nanpo.window.api.publiccontent.PublicViews.HomestayView;
import cn.nanpo.window.api.publiccontent.PublicViews.ProductDetailView;
import cn.nanpo.window.api.publiccontent.PublicViews.ProductSummaryView;
import cn.nanpo.window.api.publiccontent.PublicViews.SiteView;
import cn.nanpo.window.api.publiccontent.PublicViews.TravelPlanView;
import cn.nanpo.window.api.publiccontent.PublicViews.TravelRouteView;
import cn.nanpo.window.common.api.PageResponse;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.PublicCatalogRepository;

@Service
@Transactional(readOnly = true)
public class PublicCatalogService {

    private final PublicCatalogRepository repository;

    public PublicCatalogService(PublicCatalogRepository repository) {
        this.repository = repository;
    }

    public SiteView site() {
        return repository.findPublishedSite()
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "村庄公开信息尚未发布"));
    }

    public List<TravelRouteView> travelRoutes() {
        return repository.findTravelRoutes();
    }

    public PageResponse<AttractionView> attractions(int page, int size) {
        PageSpec spec = pageSpec(page, size);
        return new PageResponse<>(repository.findAttractions(spec.size(), spec.offset()),
                spec.page(), spec.size(), repository.countAttractions());
    }

    public List<TravelPlanView> travelPlans() {
        return repository.findTravelPlans();
    }

    public PageResponse<HomestayView> homestays(int page, int size) {
        PageSpec spec = pageSpec(page, size);
        return new PageResponse<>(repository.findHomestays(spec.size(), spec.offset()),
                spec.page(), spec.size(), repository.countHomestays());
    }

    public PageResponse<ExperienceView> experiences(int page, int size) {
        PageSpec spec = pageSpec(page, size);
        return new PageResponse<>(repository.findExperiences(spec.size(), spec.offset()),
                spec.page(), spec.size(), repository.countExperiences());
    }

    public PageResponse<ProductSummaryView> products(int page, int size) {
        PageSpec spec = pageSpec(page, size);
        return new PageResponse<>(repository.findProducts(spec.size(), spec.offset()),
                spec.page(), spec.size(), repository.countProducts());
    }

    public ProductDetailView product(long productId) {
        return repository.findProduct(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "农品不存在或已下线"));
    }

    public FarmerDetailView farmer(long farmerId) {
        return repository.findFarmerDetail(farmerId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "农户不存在或未公开"));
    }

    private PageSpec pageSpec(int page, int size) {
        if (page < 1) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "size 必须在 1 到 100 之间");
        }
        return new PageSpec(page, size, (page - 1) * size);
    }

    private record PageSpec(int page, int size, int offset) {
    }
}

