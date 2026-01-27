package sad.storereg.repo.appdata;

import org.springframework.data.jpa.repository.JpaRepository;

import sad.storereg.models.appdata.IssueItem;

public interface IssueItemRepository extends JpaRepository<IssueItem, Long>{

}
