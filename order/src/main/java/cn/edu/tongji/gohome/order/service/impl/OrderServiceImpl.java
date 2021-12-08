package
        cn.edu.tongji.gohome.order.service.impl;

import cn.edu.tongji.gohome.order.dto.*;
import cn.edu.tongji.gohome.order.dto.mapper.OrderDetailedInfoMapper;
import cn.edu.tongji.gohome.order.dto.mapper.OrderInfoMapper;
import cn.edu.tongji.gohome.order.model.*;
import cn.edu.tongji.gohome.order.repository.*;
import cn.edu.tongji.gohome.order.service.OrderService;
import com.github.yitter.idgen.YitIdHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * implements the service for order mainly.
 *
 * @className: OrderServiceImpl
 * @author: loey
 * @date: 2021-11-21 17:04
 **/
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private CustomerCommentRepository customerCommentRepository;

    @Resource
    private OrderStayRepository orderStayRepository;

    @Resource
    private RoomPhotoRepository roomPhotoRepository;

    @Resource
    private VOrderStayRepository vOrderStayRepository;

    @Resource
    private StayRepository stayRepository;

    @Resource
    private ViewCouponInformationRepository viewCouponInformationRepository;

    @Resource
    private CouponRepository couponRepository;

    @Resource
    private ViewOrderTimeRepository viewOrderTimeRepository;

    @Resource
    private ViewStayCustomerRepository stayCustomerRepository;

    @Resource
    private OrderReportRepository orderReportRepository;

    /**
     * @param order: orderEntity to get orderId and other information...
     * @description: A private function for searching order information list... to reduce the same code...
     * @return: cn.edu.tongji.gohome.order.dto.OrderInfoDto
     * @author: leoy
     * @date: 2021/11/22 19:31
     **/
    private OrderInfoDto getInfoFromOrderEntity(OrderEntity order) {

        OrderInfoDto orderInfo = new OrderInfoDto();
        long orderId = order.getOrderId();

        //set order info into orderInfoDto.
        orderInfo.setOrderId(orderId);
        orderInfo.setOrderStatus(order.getOrderStatus());
        orderInfo.setOrderTime(order.getOrderTime());
        orderInfo.setTotalCost(order.getTotalCost());

        //get order stayScore.
        CustomerCommentEntity customerComment = customerCommentRepository.findFirstByOrderId(orderId);
        if (customerComment != null) {
            orderInfo.setStayScore(customerComment.getStayScore());
            orderInfo.setCustomerCommentContent(customerComment.getCustomerCommentContent());
        } else {
            orderInfo.setStayScore(-1);
            orderInfo.setCustomerCommentContent("");
        }

        //get order startTime, endTime.
        ViewOrderTimeEntity viewOrderTime = viewOrderTimeRepository.findFirstByOrderId(orderId);
        if(viewOrderTime != null){
            orderInfo.setOrderStartTime(viewOrderTime.getMinStartTime());
            orderInfo.setOrderEndTime(viewOrderTime.getMaxEndTime());
        }

        OrderStayEntity orderStay = orderStayRepository.findFirstByOrderId(orderId);

        RoomPhotoEntity roomPhoto = null;
        if (orderStay != null) {

            //get first room photo link.
            roomPhoto = roomPhotoRepository.findFirstByStayIdAndRoomId(orderStay.getStayId(), orderStay.getRoomId());
            if(roomPhoto != null){
                orderInfo.setRoomPhotoLink(roomPhoto.getRoomPhotoLink());
            }
            else{
                orderInfo.setRoomPhotoLink("");
            }

            //get stayName,stayAddress,hostAvatarLink,hostName
            StayEntity stay = stayRepository.findFirstByStayId(orderStay.getStayId());
            orderInfo.setStayId(stay.getStayId());
            orderInfo.setStayName(stay.getStayName());
            orderInfo.setDetailedAddress(stay.getDetailedAddress());

            //get stayProvince and stayCity from detailedAddress.
            System.out.println(stay.getDetailedAddress());
            orderInfo.setStayProvince("");
            orderInfo.setStayCity("");
            if(stay.getDetailedAddress() != null){
                String regEx = "(.*?)省(.*?)市.*|(.*?)市(.*?区).*";
                Pattern pattern = Pattern.compile(regEx);
                Matcher matcher = pattern.matcher(stay.getDetailedAddress());
                if(matcher.find()){
                    System.out.println(matcher.group(1));
                    orderInfo.setStayProvince(matcher.group(1));
                    orderInfo.setStayCity(matcher.group(2));
                }
            }

            ViewStayCustomerEntity viewStayCustomer = stayCustomerRepository.getById(orderStay.getStayId());
            orderInfo.setHostAvatarLink(viewStayCustomer.getCustomerAvatarLink());
            orderInfo.setHostName(viewStayCustomer.getCustomerName());
        }

        //set reportInfo : reportStatus,reportReason,reportReply
        int reportStatus = -1;
        String reportReason = "";
        String reportReply = "";
        OrderReportEntity orderReport = orderReportRepository.findByOrderId(orderId);
        if(orderInfo.getOrderStatus() == OrderStatus.ORDER_BUSINESS_COMPLETED){
            reportStatus = 0;
        }
        if(orderReport != null){
            boolean status = orderReport.isDealt();
            if(status){
               reportStatus = 2;
               reportReason = orderReport.getReportReason();
               reportReply = orderReport.getReply();
            }
            else{
                reportStatus = 1;
                reportReason = orderReport.getReportReason();
            }
        }
        orderInfo.setReportStatus(reportStatus);
        orderInfo.setReportReason(reportReason);
        orderInfo.setReportReply(reportReply);

        return orderInfo;
    }

    /**
     * @param customerId : customer's id.
     * @description: the implements for searching the order information by customer id...
     * @return: java.util.List<cn.edu.tongji.gohome.order.dto.OrderInfoDto>
     * @author: leoy
     * @date: 2021/11/21 17:07
     **/
    @Override
    public HashMap<String, Object> searchOrderInfoForCustomerId(long customerId, Integer currentPage, Integer pageSize) {

        Pageable pageable = PageRequest.of(currentPage, pageSize);

        HashMap<String, Object> results = new HashMap<>();

        List<OrderInfoDto> orderInfoDtoList = new ArrayList<>();
        Page<OrderEntity> orderEntityList = orderRepository.findAllByCustomerId(customerId, pageable);

        // there is no order for customer whose id = ...
        if (orderEntityList == null) {
            results.put("totalPage", 0);
            results.put("orderInfo", orderInfoDtoList);
            return results;
        }

        results.put("totalPage", orderEntityList.getTotalPages());

        for (OrderEntity order : orderEntityList) {
            orderInfoDtoList.add(getInfoFromOrderEntity(order));
        }

        results.put("orderInfo", orderInfoDtoList);
        return results;

    }

    /**
     * @param stayId:      stay id for searching the order.
     * @param currentPage: current page for info.
     * @param pageSize:    the count of data every page.
     * @description: search the order info for the host when he clicks one stay...
     * @return: java.util.HashMap<java.lang.String, java.lang.Object>
     * @author: leoy
     * @date: 2021/11/22 10:59
     **/
    @Override
    public HashMap<String, Object> searchOrderInfoForStayId(long stayId, Integer currentPage, Integer pageSize) {

        // get all the order info
        Pageable pageable = PageRequest.of(currentPage, pageSize);

        HashMap<String, Object> results = new HashMap<>();
        List<OrderInfoDto> orderInfoDtoList = new ArrayList<>();

        Page<VOrderStayEntity> vOrderStayEntityPageable = vOrderStayRepository.findAllByStayId(stayId, pageable);

        // there is no order for customer whose id = ...
        if (vOrderStayEntityPageable == null) {
            results.put("totalPage", 0);
            results.put("orderInfo", orderInfoDtoList);
            return results;
        }
        results.put("totalPage", vOrderStayEntityPageable.getTotalPages());
        List<OrderEntity> orderEntityList = new ArrayList<>();
        for (VOrderStayEntity vOrderStayEntity : vOrderStayEntityPageable) {
            orderEntityList.add(orderRepository.findFirstByOrderId(vOrderStayEntity.getOrderId()));
        }

        for (OrderEntity order : orderEntityList) {
            orderInfoDtoList.add(getInfoFromOrderEntity(order));
        }
        results.put("orderInfo", orderInfoDtoList);
        return results;
    }


    /**
     * @param orderId:    the id for order...
     * @param currentPage the current page of all...
     * @param pageSize:   number for the record every page...
     * @description: When the customer or host clicks the order, the back-end should return the detailed information for order and stay-room...
     * @return: java.util.HashMap<java.lang.String, java.lang.Object>
     * @author: leoy
     * @date: 2021/11/22 19:33
     **/
    @Override
    public HashMap<String, Object> searchOrderDetailedInfoForOrderId(long orderId, Integer currentPage, Integer pageSize) {

        // get all room info by order id...
        HashMap<String, Object> results = new HashMap<>();
        List<OrderDetailedInfoDto> orderDetailedInfoDtoList = new ArrayList<>();

        Pageable pageable = PageRequest.of(currentPage, pageSize);
        Page<OrderStayEntity> orderStayEntityPage = orderStayRepository.findAllByOrderId(orderId, pageable);

        if (orderStayEntityPage == null) {
            results.put("totalPage", 0);
            return results;
        }

        results.put("totalPage", orderStayEntityPage.getTotalPages());

        long stayId = vOrderStayRepository.findFirstByOrderId(orderId).getStayId();
        String stayName = stayRepository.findFirstByStayId(stayId).getStayName();
        results.put("stayName", stayName);

        for (OrderStayEntity stayInfo : orderStayEntityPage) {

            int roomId = stayInfo.getRoomId();
            RoomPhotoEntity roomPhoto = roomPhotoRepository.findFirstByStayIdAndRoomId(stayId, roomId);
            orderDetailedInfoDtoList.add(OrderDetailedInfoMapper.getInstance().toDto(stayInfo, roomPhoto));
        }
        results.put("orderDetailedInfo", orderDetailedInfoDtoList);

        return results;
    }

    @Override
    public HashMap<String, Object> searchOrderInfoForCustomerIdAndOrderStatus(long customerId, int orderStatus,
                                                                              Integer currentPage, Integer pageSize) {
        Pageable pageable = PageRequest.of(currentPage, pageSize);

        HashMap<String, Object> results = new HashMap<>();

        List<OrderInfoDto> orderInfoDtoList = new ArrayList<>();
        Page<OrderEntity> orderEntityList = orderRepository.findAllByCustomerIdAndOrderStatus(customerId, orderStatus, pageable);

        // there is no order for customer whose id = ...
        if (orderEntityList == null) {
            results.put("totalPage", 0);
            results.put("orderInfo", orderInfoDtoList);
            return results;
        }

        results.put("totalPage", orderEntityList.getTotalPages());

        for (OrderEntity order : orderEntityList) {
            orderInfoDtoList.add(getInfoFromOrderEntity(order));
        }

        results.put("orderInfo", orderInfoDtoList);
        return results;
    }


    private OrderEntity getInformationFromOrderContent(OrderContent orderContent) {

        OrderEntity order = new OrderEntity();

        order.setOrderId(YitIdHelper.nextId());
        order.setOrderTime(orderContent.getOrderTime());
        order.setCustomerId(orderContent.getCustomerId());
        order.setMemberAmount(orderContent.getMemberAmount());
        order.setTotalCost(orderContent.getTotalCost());
        order.setOrderStatus(orderContent.getOrderStatus());

        return order;
    }

    private List<OrderStayEntity> getRoomInfoFromOrderContent(long orderId, List<OrderStayInfoDto> orderStayInfoDtoList) {
        List<OrderStayEntity> orderStayEntityList = new ArrayList<>();

        for (OrderStayInfoDto orderStayInfoDto : orderStayInfoDtoList) {
            OrderStayEntity orderStayEntity = new OrderStayEntity();
            orderStayEntity.setOrderId(orderId);
            orderStayEntity.setStayId(orderStayInfoDto.getStayId());
            orderStayEntity.setRoomId(orderStayInfoDto.getRoomId());
            orderStayEntity.setStartTime(orderStayInfoDto.getStartTime());
            orderStayEntity.setEndTime(orderStayInfoDto.getEndTime());
            orderStayEntity.setMoneyAmount(orderStayInfoDto.getMoneyAmount());
            orderStayEntityList.add(orderStayEntity);
        }
        return orderStayEntityList;
    }


    /**
     * add one order in db...
     *
     * @param orderContent: the detailed information for order.
     * @return : void
     * @author : leoy
     * @since : 2021/11/23 22:28
     **/
    @Override
    public Long addOrderAndDetailedInformation(OrderContent orderContent) {

        OrderEntity order = getInformationFromOrderContent(orderContent);
        List<OrderStayEntity> orderStayEntityList = getRoomInfoFromOrderContent(order.getOrderId(), orderContent.getOrderStayEntityList());
        orderRepository.save(order);
        orderStayRepository.saveAll(orderStayEntityList);

        return order.getOrderId();
    }

    @Override
    public void updateOrderStatus(long orderId, int orderStatus) {

        OrderEntity order = orderRepository.getById(orderId);
        order.setOrderStatus(orderStatus);
        System.out.println("orderStatus: "+orderStatus);
        orderRepository.save(order);
    }

    public Map<String, Object> searchUsableCouponForCustomerId(long customerId, BigDecimal couponLimit, Integer currentPage, Integer pageSize) {
        Pageable pageable = PageRequest.of(currentPage, pageSize);

        Specification<ViewCouponInformationEntity> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            //customer_id
            predicateList.add(criteriaBuilder.equal(root.get("customerId"), customerId));
            //status
            predicateList.add(criteriaBuilder.equal(root.get("couponStatus"), CouponStatus.COUPON_UNUSED));
            //couponLimit
            predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("couponLimit"), couponLimit));

            Predicate[] pre = new Predicate[predicateList.size()];
            pre = predicateList.toArray(pre);
            return query.where(pre).getRestriction();
        };
        Page<ViewCouponInformationEntity> page = viewCouponInformationRepository.findAll(specification, pageable);
        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", page.getTotalPages());
        map.put("couponInfo", page.toList());
        return map;
    }

    public void updateOCouponStatus(long couponId, int couponStatus) {

        CouponEntity coupon = couponRepository.getById(couponId);
        coupon.setCouponStatus(couponStatus);
        couponRepository.save(coupon);
    }
}