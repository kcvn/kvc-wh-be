package com.kcvn.spm.app.order.service

import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.repository.OrderDetailRepository
import com.kcvn.spm.repository.OrderRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

@Service
@Transactional
class OrderService (
    private val orderRep: OrderRepository,
    private val orderDetailRep: OrderDetailRepository
) {
    fun getPaginatedCompletionRateProduct(
        request: OrderSearchRequest?,
        pageable: Pageable?
    ): PagingOrderResponse
    {
        val calendarResponses = mutableListOf<CalendarValueResponse>()

        if (request?.startDate != null && request.endDate != null) {

            var currentDate = request.startDate
            while (!currentDate!!.isAfter(request.endDate)) {
                val response = CalendarValueResponse(
                    key = currentDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                    value = currentDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                    isHoliday = currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY
                )
                calendarResponses.add(response)

                currentDate = currentDate.plusDays(1)
            }
        }

        val pagingOrderResponse = PagingOrderResponse()
        pagingOrderResponse.columns = calendarResponses

        val listOrderResponse = orderRep.getPaginatedCompletionRateProduct(request,pageable)
        pagingOrderResponse.data = listOrderResponse.first
        pagingOrderResponse.totalRecords = listOrderResponse.second



        return pagingOrderResponse

    }

}