package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.config.Site;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    //List<Page> findAllBySite(SiteModel site);
    //Integer countBySite(SiteModel siteId);

    Page findByPath(String path);
}
