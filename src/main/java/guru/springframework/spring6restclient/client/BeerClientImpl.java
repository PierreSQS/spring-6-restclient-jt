package guru.springframework.spring6restclient.client;


import guru.springframework.spring6restclient.model.BeerDTO;
import guru.springframework.spring6restclient.model.BeerDTOPageImpl;
import guru.springframework.spring6restclient.model.BeerStyle;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Created by jt, Spring Framework Guru.
 */
@Service
@RequiredArgsConstructor
public class BeerClientImpl implements BeerClient {

    public static final String GET_BEER_PATH = "/api/v1/beer";
    public static final String GET_BEER_BY_ID_PATH = "/api/v1/beer/{beerId}";

    private final RestClient.Builder restClientBuilder;

    @Override
    public BeerDTOPageImpl listBeers() {
        RestClient restClient = restClientBuilder.build();

        return restClient.get()
                .uri(GET_BEER_PATH)
                .retrieve()
                .body(BeerDTOPageImpl.class);
    }

    @Override
    public BeerDTOPageImpl listBeers(String beerName, BeerStyle beerStyle, Boolean showInventory, Integer pageNumber, Integer pageSize) {
        RestClient restClient = restClientBuilder.build();

        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path(GET_BEER_PATH)
                        .queryParamIfPresent("beerName", java.util.Optional.ofNullable(beerName))
                        .queryParamIfPresent("beerStyle", java.util.Optional.ofNullable(beerStyle))
                        .queryParamIfPresent("showInventory", java.util.Optional.ofNullable(showInventory))
                        .queryParamIfPresent("pageNumber", java.util.Optional.ofNullable(pageNumber))
                        .queryParamIfPresent("pageSize", java.util.Optional.ofNullable(pageSize))
                        .build())
                .retrieve()
                .body(BeerDTOPageImpl.class);
    }

    @Override
    public BeerDTO getBeerById(UUID beerId) {
        RestClient restClient = restClientBuilder.build();

        return restClient.get()
                .uri(GET_BEER_BY_ID_PATH, beerId)
                .retrieve()
                .body(BeerDTO.class);
    }

    @Override
    public BeerDTO createBeer(BeerDTO newDto) {
        RestClient restClient = restClientBuilder.build();

        val location = restClient.post()
                .uri(uriBuilder -> uriBuilder.path(GET_BEER_PATH).build())
                .body(newDto)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getLocation();

        assert location != null;
        return restClient.get()
                .uri(location.getPath())
                .retrieve()
                .body(BeerDTO.class);
    }

    @Override
    public BeerDTO updateBeer(BeerDTO beerDto) {

        RestClient restClient = restClientBuilder.build();

        restClient.put()
                .uri(GET_BEER_BY_ID_PATH, beerDto.getId())
                .body(beerDto)
                .retrieve()
                .toBodilessEntity();

        return getBeerById(beerDto.getId());
    }

    @Override
    public void deleteBeer(UUID beerId) {

        RestClient restClient = restClientBuilder.build();

        restClient.delete()
                .uri(GET_BEER_BY_ID_PATH, beerId)
                .retrieve()
                .toBodilessEntity();

    }
}












