package codex.mmxxvi.services.impl;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.mongodb.client.result.DeleteResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import codex.mmxxvi.dto.request.IndexTicketRequest;
import codex.mmxxvi.dto.request.IndexTicketItemRequest;
import codex.mmxxvi.dto.request.PageRequestDto;
import codex.mmxxvi.dto.request.SearchTicketRequest;
import codex.mmxxvi.dto.response.PageResponse;
import codex.mmxxvi.dto.response.SearchTicketResponse;
import codex.mmxxvi.entity.SearchTicketDocument;
import codex.mmxxvi.entity.SearchTicketItemDocument;
import codex.mmxxvi.exception.AppExceptions;
import codex.mmxxvi.services.TicketSearchService;
import reactor.core.publisher.Mono;

@Service
public class TicketSearchServiceImpl implements TicketSearchService {
        private final ReactiveMongoTemplate mongoTemplate;
        private final ObjectMapper objectMapper;
        private final String ticketCollection;

        public TicketSearchServiceImpl(
                ReactiveMongoTemplate mongoTemplate,
                ObjectMapper objectMapper,
                @Value("${search.mongodb.ticket-collection:ticket-search}") String ticketCollection
        ) {
                this.mongoTemplate = mongoTemplate;
                this.objectMapper = objectMapper;
                this.ticketCollection = ticketCollection;
        }

        @Override
        public Mono<PageResponse<SearchTicketResponse>> searchTickets(
                String keyword,
                SearchTicketRequest searchTicketRequest,
                PageRequestDto pageRequestDto
        ) {
                int pageNo = pageRequestDto.getPageNo();
                int pageSize = pageRequestDto.getPageSize();
                Query countQuery = buildQuery(keyword, searchTicketRequest);
                Query pageQuery = buildQuery(keyword, searchTicketRequest)
                        .with(Sort.by(
                                Sort.Order.asc("dateStart"),
                                Sort.Order.desc("updatedAt")
                        ))
                        .skip((long) pageNo * pageSize)
                        .limit(pageSize);

                Mono<Long> totalElementsMono = mongoTemplate.count(countQuery, SearchTicketDocument.class, ticketCollection);
                Mono<List<SearchTicketResponse>> contentMono = mongoTemplate.find(pageQuery, SearchTicketDocument.class, ticketCollection)
                        .map(this::toResponse)
                        .collectList();

                return Mono.zip(totalElementsMono, contentMono)
                        .map(tuple -> toPageResponse(tuple.getT2(), tuple.getT1(), pageNo, pageSize));
        }

        @Override
        public Mono<Void> indexTicket(IndexTicketRequest request) {
                if (request.getId() == null) {
                        return Mono.error(new AppExceptions.BadRequestException("Ticket id is required"));
                }

                SearchTicketDocument document = toDocument(request);
                return mongoTemplate.save(document, ticketCollection)
                        .then();
        }

        @Override
        public Mono<Void> deleteTicket(UUID ticketId) {
                Query query = Query.query(Criteria.where("_id").is(ticketId));
                return mongoTemplate.remove(query, SearchTicketDocument.class, ticketCollection)
                        .map(DeleteResult::getDeletedCount)
                        .then();
        }

        private Query buildQuery(String keyword, SearchTicketRequest searchTicketRequest) {
                List<Criteria> criteria = new java.util.ArrayList<>();
                criteria.add(Criteria.where("deletedAt").is(null));

                if (keyword != null && !keyword.isBlank()) {
                        Pattern keywordPattern = containsPattern(keyword);
                        criteria.add(new Criteria().orOperator(
                                Criteria.where("title").regex(keywordPattern),
                                Criteria.where("trainNumber").regex(keywordPattern),
                                Criteria.where("departureStationName").regex(keywordPattern),
                                Criteria.where("arrivalStationName").regex(keywordPattern),
                                Criteria.where("journeyNote").regex(keywordPattern),
                                Criteria.where("ticketItems.name").regex(keywordPattern),
                                Criteria.where("ticketItems.description").regex(keywordPattern),
                                Criteria.where("ticketItems.coachCode").regex(keywordPattern),
                                Criteria.where("ticketItems.seatClass").regex(keywordPattern),
                                Criteria.where("ticketItems.seatType").regex(keywordPattern)
                        ));
                }

                if (searchTicketRequest != null) {
                        addStationFilter(criteria, "departureStationName", "departureStationCode", searchTicketRequest.getDepartureStation());
                        addStationFilter(criteria, "arrivalStationName", "arrivalStationCode", searchTicketRequest.getArrivalStation());

                        if (searchTicketRequest.getDepartureDate() != null) {
                                criteria.add(Criteria.where("dateStart")
                                        .gte(searchTicketRequest.getDepartureDate().atStartOfDay())
                                        .lt(searchTicketRequest.getDepartureDate().plusDays(1).atStartOfDay()));
                        }
                }

                criteria.add(Criteria.where("ticketItems").elemMatch(buildTicketItemCriteria(searchTicketRequest)));

                Query query = new Query();
                if (criteria.size() == 1) {
                        query.addCriteria(criteria.getFirst());
                } else {
                        query.addCriteria(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
                }
                return query;
        }

        private Criteria buildTicketItemCriteria(SearchTicketRequest searchTicketRequest) {
                List<Criteria> criteria = new java.util.ArrayList<>();
                criteria.add(Criteria.where("stockAvailable").gte(1));

                if (searchTicketRequest != null) {
                        addRegexCriteria(criteria, "coachCode", searchTicketRequest.getCoachCode());
                        addRegexCriteria(criteria, "seatClass", searchTicketRequest.getSeatClass());
                        addRegexCriteria(criteria, "seatType", searchTicketRequest.getSeatType());

                        Long minPrice = searchTicketRequest.getMinPrice();
                        Long maxPrice = searchTicketRequest.getMaxPrice();
                        if (minPrice != null || maxPrice != null) {
                                List<Criteria> priceCriteria = new java.util.ArrayList<>();
                                priceCriteria.add(buildRangeCriteria("priceFlash", minPrice, maxPrice));
                                priceCriteria.add(buildRangeCriteria("priceOriginal", minPrice, maxPrice));
                                criteria.add(new Criteria().orOperator(priceCriteria.toArray(Criteria[]::new)));
                        }
                }

                if (criteria.size() == 1) {
                        return criteria.getFirst();
                }

                return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
        }

        private Criteria buildRangeCriteria(String field, Long minPrice, Long maxPrice) {
                Criteria criteria = Criteria.where(field);
                if (minPrice != null) {
                        criteria = criteria.gte(minPrice);
                }
                if (maxPrice != null) {
                        criteria = criteria.lte(maxPrice);
                }
                return criteria;
        }

        private void addStationFilter(List<Criteria> criteria, String stationField, String codeField, String value) {
                if (value == null || value.isBlank()) {
                        return;
                }

                Pattern stationPattern = containsPattern(value);
                criteria.add(new Criteria().orOperator(
                        Criteria.where(stationField).regex(stationPattern),
                        Criteria.where(codeField).regex(stationPattern)
                ));
        }

        private void addRegexCriteria(List<Criteria> criteria, String field, String value) {
                if (value == null || value.isBlank()) {
                        return;
                }

                criteria.add(Criteria.where(field).regex(containsPattern(value)));
        }

        private Pattern containsPattern(String value) {
                return Pattern.compile(Pattern.quote(value.trim()), Pattern.CASE_INSENSITIVE);
        }

        private PageResponse<SearchTicketResponse> toPageResponse(List<SearchTicketResponse> content, long totalElements, int pageNo, int pageSize) {
                int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
                boolean last = totalPages == 0 || pageNo >= totalPages - 1;

                return PageResponse.<SearchTicketResponse>builder()
                        .content(content)
                        .pageNo(pageNo)
                        .pageSize(pageSize)
                        .totalElements(totalElements)
                        .totalPages(totalPages)
                        .last(last)
                        .build();
        }

        private SearchTicketDocument toDocument(IndexTicketRequest request) {
                return SearchTicketDocument.builder()
                        .id(request.getId())
                        .title(request.getTitle())
                        .trainNumber(request.getTrainNumber())
                        .departureStationCode(request.getDepartureStationCode())
                        .departureStationName(request.getDepartureStationName())
                        .arrivalStationCode(request.getArrivalStationCode())
                        .arrivalStationName(request.getArrivalStationName())
                        .journeyNote(request.getJourneyNote())
                        .dateStart(request.getDateStart())
                        .dateEnd(request.getDateEnd())
                        .status(request.getStatus())
                        .ticketItems(toTicketItemDocuments(request.getTicketItems()))
                        .createdAt(request.getCreatedAt())
                        .updatedAt(request.getUpdatedAt())
                        .deletedAt(request.getDeletedAt())
                        .build();
        }

        private List<SearchTicketItemDocument> toTicketItemDocuments(List<IndexTicketItemRequest> items) {
                if (items == null) {
                        return List.of();
                }

                return items.stream()
                        .map(item -> SearchTicketItemDocument.builder()
                                .id(item.getId())
                                .ticketId(item.getTicketId())
                                .name(item.getName())
                                .description(item.getDescription())
                                .coachCode(item.getCoachCode())
                                .seatClass(item.getSeatClass())
                                .seatType(item.getSeatType())
                                .seatLabels(item.getSeatLabels())
                                .availableSeatLabels(item.getAvailableSeatLabels())
                                .stockInitial(item.getStockInitial())
                                .stockAvailable(item.getStockAvailable())
                                .stockPrepared(item.getStockPrepared())
                                .priceOriginal(item.getPriceOriginal())
                                .priceFlash(item.getPriceFlash())
                                .saleStartTime(item.getSaleStartTime())
                                .saleEndTime(item.getSaleEndTime())
                                .createdAt(item.getCreatedAt())
                                .updatedAt(item.getUpdatedAt())
                                .deletedAt(item.getDeletedAt())
                                .build())
                        .toList();
        }

        private SearchTicketResponse toResponse(SearchTicketDocument document) {
                SearchTicketResponse response = objectMapper.convertValue(document, SearchTicketResponse.class);
                response.setScore(null);
                return response;
        }
}
