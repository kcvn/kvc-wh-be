package com.kcvn.spm.app.order.service

import com.kcvn.spm.repository.OrderDetailRepository
import com.kcvn.spm.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OrderService (
    private val orderRep: OrderRepository,
    private val orderDetailRep: OrderDetailRepository
) {
}