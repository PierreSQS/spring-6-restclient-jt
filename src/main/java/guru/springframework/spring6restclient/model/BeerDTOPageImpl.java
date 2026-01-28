package guru.springframework.spring6restclient.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Adapted by ChatGpt5.2, 2026-01-27.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeerDTOPageImpl extends PageImpl<BeerDTO> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public BeerDTOPageImpl(
            @JsonProperty("content") List<BeerDTO> content,
            @JsonProperty("page") PageMeta page) {

        super(
                content == null ? List.of() : content,
                PageRequest.of(page.number, page.size),
                page.totalElements
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageMeta {
        public final int number;
        public final int size;
        public final long totalElements;

        @JsonCreator
        public PageMeta(
                @JsonProperty("number") int number,
                @JsonProperty("size") int size,
                @JsonProperty("totalElements") long totalElements) {
            this.number = number;
            this.size = size;
            this.totalElements = totalElements;
        }
    }
}
