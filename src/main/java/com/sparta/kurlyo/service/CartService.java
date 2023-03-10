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
        // ???????????? ????????? ???????????? ???
        CartWholeResponseDto dto = new CartWholeResponseDto();
        // ?????? ???????????? ???????????? ????????? ????????? ???
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

        // ??????
        // ????????? ????????? ???????????? ?????????
        // isPlus == true : ????????? ?????? ????????? ????????????.
        // isPlust == false : ????????? ?????? ????????? ????????????.

        Members member = membersRepository.findByMemberName(userDetailsImpl.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("?????? ???????????? ???????????? ????????????.")
        );

        Cart cart = cartRepository.findById(cartId).orElseThrow(
                () -> new IllegalArgumentException("?????? ??????????????? ???????????? ????????????.")
        );

        // ???????????? ???????????? ??????
        if (member.getId() != cart.getMember().getId()) {
            throw new AccessDeniedException("????????? ????????????.");
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
                () -> new IllegalArgumentException("?????? ???????????? ???????????? ????????????.")
        );

        Cart cart = cartRepository.findById(cartId).orElseThrow(
                () -> new IllegalArgumentException("?????? ??????????????? ???????????? ????????????.")
        );

        if (member.getId() != cart.getMember().getId()) {
            throw new AccessDeniedException("????????? ????????????.");
        }

        cartRepository.delete(cart);
    }
}
//