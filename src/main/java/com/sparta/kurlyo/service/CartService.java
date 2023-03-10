package com.sparta.kurlyo.service;

import com.sparta.kurlyo.dto.CartRequestDto;
import com.sparta.kurlyo.dto.CartResponseDto;
import com.sparta.kurlyo.dto.CartWholeResponseDto;
import com.sparta.kurlyo.dto.CustomException;
import com.sparta.kurlyo.dto.ExceptionMessage;
import com.sparta.kurlyo.dto.Response;
import com.sparta.kurlyo.dto.SuccessMessage;
import com.sparta.kurlyo.entity.Cart;
import com.sparta.kurlyo.entity.Goods;
import com.sparta.kurlyo.entity.Members;
import com.sparta.kurlyo.repository.CartRepository;
import com.sparta.kurlyo.repository.GoodsRepository;
import com.sparta.kurlyo.repository.MembersRepository;
import com.sparta.kurlyo.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final MembersRepository membersRepository;
    private final GoodsRepository goodsRepository;

    @Transactional
    public ResponseEntity<Response> addCart(long goodsId, String username) {
        Members member = getMember(username);
        Optional<Cart> cart = cartRepository.findByGoods_IdAndMember_Account(goodsId, username);
        if (cart.isPresent()) {
            cart.get().addAmount();
        } else {
            Goods goods = getGoods(goodsId);
            cartRepository.save(new Cart(member, goods));
        }
        return Response.toResponseEntity(SuccessMessage.ADD_CART_SUCCESS);
    }

    private Goods getGoods(long goodsId) {
        return goodsRepository.findById(goodsId).orElseThrow(
                () -> new CustomException(ExceptionMessage.GOODS_NOT_FOUND)
        );
    }

    private Members getMember(String username) {
        return membersRepository.findByAccount(username).orElseThrow(
                () -> new CustomException(ExceptionMessage.UNAUTHORIZED_MEMBER)
        );
    }


    @Transactional(readOnly = true)
    public CartWholeResponseDto getCart(Members member){
        // 장바구니 목록을 가져오는 것
        CartWholeResponseDto dto = new CartWholeResponseDto();
        // 특정 사용자의 장바구니 목록을 가지고 옴
        List<Cart> cartList = cartRepository.findByMember(member);
        for (Cart cart : cartList){
            dto.addGoodsCart(cart);
        }
        return dto;
    }

    @Transactional
    public CartResponseDto updateGoodsCart
            (Long cartId,
             CartRequestDto requestDto,
             UserDetailsImpl userDetailsImpl)
    {

        // 기능
        // 카트의 상품을 추가하기 위해서
        // isPlus == true : 카트의 상품 수량이 증가한다.
        // isPlust == false : 카트의 상품 수량이 감소한다.

        Members member = membersRepository.findByMemberName(userDetailsImpl.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다.")
        );

        Cart cart = cartRepository.findById(cartId).orElseThrow(
                () -> new IllegalArgumentException("해당 장바구니가 존재하지 않습니다.")
        );

        // 사용자의 장바구니 확인
        if (member.getId() != cart.getMember().getId()) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        // isPlus == true : cart.amount += 1
        // isPlus == false : cart.amount -= 1
        if (requestDto.isPlus()) {
            cart.update(1);
        } else {
            cart.update(-1);
        }

        return CartResponseDto.of(cart);
    }

    @Transactional
    public void deleteGoodsCart
            (Long cartId,
             UserDetailsImpl userDetailsImpl)
    {
        Members member = membersRepository.findByMemberName(userDetailsImpl.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다.")
        );

        Cart cart = cartRepository.findById(cartId).orElseThrow(
                () -> new IllegalArgumentException("해당 장바구니가 존재하지 않습니다.")
        );

        if (member.getId() != cart.getMember().getId()) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        cartRepository.delete(cart);
    }
}
//