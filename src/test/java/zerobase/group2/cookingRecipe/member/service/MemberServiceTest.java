package zerobase.group2.cookingRecipe.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import zerobase.group2.cookingRecipe.common.exception.CustomException;
import zerobase.group2.cookingRecipe.common.type.ErrorCode;
import zerobase.group2.cookingRecipe.like.repository.LikeRepository;
import zerobase.group2.cookingRecipe.member.component.MailComponent;
import zerobase.group2.cookingRecipe.member.dto.MemberDto;
import zerobase.group2.cookingRecipe.member.entity.Member;
import zerobase.group2.cookingRecipe.member.repository.MemberRepository;
import zerobase.group2.cookingRecipe.member.type.MemberStatus;
import zerobase.group2.cookingRecipe.recipe.repository.RecipeRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private MailComponent mailComponent;

    @InjectMocks
    private MemberService memberService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
            .email("group2@gmail.com")
            .name("g2")
            .emailAuthKey(UUID.randomUUID().toString())
            .emailAuthDue(LocalDateTime.now().plusMinutes(1))
            .status(MemberStatus.BEFORE_AUTH)
            .registeredAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }


    @Test
    @DisplayName("???????????? ??????")
    void successRegister() {
        //given
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.empty());
        given(memberRepository.save(any()))
            .willReturn(member);
        given(mailComponent.sendMail(anyString(), anyString(), anyString()))
            .willReturn(true);

        //when
        MemberDto memberDto = memberService.register("1", "1", "1");

        //then
        assertEquals(memberDto.getEmail(), member.getEmail());
        assertEquals(memberDto.getName(), member.getName());
    }

    @Test
    @DisplayName("???????????? ?????? - ?????? ????????? ?????? ?????? ???")
    void failedRegister_emailAlreadyInUse() {
        //given
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));

        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.register("1", "1", "1"));

        //then
        assertEquals(ErrorCode.EMAIL_ALREADY_REGISTERED, exception.getError());
    }

    @Test
    @DisplayName("????????? ?????? ??????")
    void successEmailAuth() {
        //given
        given(memberRepository.findByEmailAuthKey(anyString()))
            .willReturn(Optional.of(member));
        //when
        memberService.emailAuth(member.getEmailAuthKey());

        //then
        assertEquals(MemberStatus.IN_USE, member.getStatus());
    }

    @Test
    @DisplayName("????????? ?????? ?????? - ?????? ????????? ??????")
    void failedEmailAuth_keyNotFound() {
        //given

        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.emailAuth("1"));
        //then
        assertEquals(ErrorCode.DATA_NOT_VALID, exception.getError());
    }

    @Test
    @DisplayName("????????? ?????? ?????? - ????????? ???????????? ??????")
    void failedEmailAuth_keyExpired() {
        //given
        member.setEmailAuthDue(LocalDateTime.now().minusSeconds(1));
        given(memberRepository.findByEmailAuthKey(anyString()))
            .willReturn(Optional.of(member));
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.emailAuth(member.getEmailAuthKey()));
        //then
        assertEquals(MemberStatus.BEFORE_AUTH, member.getStatus());
        assertEquals(ErrorCode.ACCESS_NOT_VALID, exception.getError());
    }

    @Test
    @DisplayName("????????? ?????? ?????? - ?????? ????????? ??????")
    void failedEmailAuth_alreadyAuthenticated() {
        //given
        member.setStatus(MemberStatus.IN_USE);
        given(memberRepository.findByEmailAuthKey(anyString()))
            .willReturn(Optional.of(member));
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.emailAuth(member.getEmailAuthKey()));
        //then
        assertTrue(LocalDateTime.now().isBefore(member.getEmailAuthDue()));
        assertEquals(ErrorCode.ACCESS_NOT_VALID, exception.getError());
    }

    @Test
    @DisplayName("?????? ?????? ?????? ??????")
    void success_getInfoById() {
        //given
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        MemberDto memberDto = memberService.getInfoById("");
        //then
        assertEquals(member.getEmail(), memberDto.getEmail());
        assertEquals(member.getName(), memberDto.getName());
        assertEquals(member.getRegisteredAt(), memberDto.getRegisteredAt());
        assertEquals(member.getUpdatedAt(), memberDto.getUpdatedAt());
    }

    @Test
    @DisplayName("?????? ?????? ?????? ?????? - ???????????? ?????? ??????")
    void fail_getInfoById() {
        //given
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.empty());
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.getInfoById("1"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getError());
    }

    @Test
    @DisplayName("?????? ?????? ?????? ??????")
    void success_editMemberInfo() {
        //given
        String name = "??????????????????????????????";
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        MemberDto memberDto = memberService.editMemberInfo("1", name);
        //then
        assertEquals(name, memberDto.getName());
    }

    @Test
    @DisplayName("?????? ?????? ?????? ?????? - ???????????? ?????? ??????")
    void fail_editMemberInfo() {
        //given
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.editMemberInfo("1", "name"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getError());
    }

    @Test
    @DisplayName("???????????? ?????? ??????")
    void success_editPassword() {
        //given
        String oldPassword = "1111";
        String newPassword = "2222";
        member.setPassword(BCrypt.hashpw(oldPassword, BCrypt.gensalt()));
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        memberService.editPassword("1", oldPassword, newPassword);
        //then
        assertEquals(BCrypt.hashpw(newPassword, member.getPassword()), member.getPassword());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? - ???????????? ?????? ??????")
    void fail_editPassword_userNotFound() {
        //given
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.editPassword("1", "1", "2"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getError());
    }

    @Test
    @DisplayName("???????????? ?????? ?????? - ???????????? ?????????")
    void fail_editPassword_passwordNotMatched() {
        //given
        member.setPassword(BCrypt.hashpw("3333", BCrypt.gensalt()));
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.editPassword("1", "1111", "2222"));
        //then
        assertEquals(ErrorCode.DATA_NOT_VALID, exception.getError());
    }

    @Test
    @DisplayName("?????? ?????? ??????")
    void success_withdraw() {
        //given
        member.setPassword(BCrypt.hashpw("1111", BCrypt.gensalt()));
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        memberService.withdraw("1", "1111");
        //then
        assertEquals("????????????", member.getName());
        assertEquals(MemberStatus.WITHDRAW, member.getStatus());
    }

    @Test
    @DisplayName("?????? ?????? ?????? - ???????????? ?????? ??????")
    void fail_withdraw_userNotFound() {
        //given
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.withdraw("1", "1111"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getError());
    }

    @Test
    @DisplayName("?????? ?????? ?????? - ???????????? ?????????")
    void fail_withdraw_passwordNotMatched() {
        //given
        member.setPassword(BCrypt.hashpw("2222", BCrypt.gensalt()));
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.withdraw("1", "1111"));
        //then
        assertEquals(ErrorCode.DATA_NOT_VALID, exception.getError());
    }

    @Test
    @DisplayName("???????????? ????????? ??? ?????? ??????")
    void success_sendResetEmail() {
        //given
        member.setStatus(MemberStatus.IN_USE);
        member.setPasswordResetKey("");
        member.setPasswordResetDue(LocalDateTime.now().minusDays(1));
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        boolean result = memberService.sendEmailToResetPassword("1");
        //then
        assertTrue(result);
        assertTrue(LocalDateTime.now().isBefore(member.getPasswordResetDue()));
        assertNotEquals("", member.getPasswordResetKey());
    }

    @Test
    @DisplayName("???????????? ????????? ??? ?????? ?????? - ???????????? ?????? ??????")
    void fail_sendResetEmail_userNotFound() {
        //given
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.sendEmailToResetPassword("1"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getError());
    }

    @Test
    @DisplayName("???????????? ????????? ??? ?????? ?????? - ????????? ??? ?????? ??????")
    void fail_sendResetEmail_keyAlreadyIssued() {
        //given
        member.setPasswordResetKey(UUID.randomUUID().toString());
        member.setPasswordResetDue(LocalDateTime.now().plusHours(1));
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.sendEmailToResetPassword("1"));
        //then
        assertEquals(ErrorCode.EMAIL_NOT_AUTHENTICATED, exception.getError());
    }

    @Test
    @DisplayName("???????????? ????????? ????????? ?????? ??????")
    void success_resetAuth() {
        //given
        member.setPasswordResetKey("1111");
        member.setPasswordResetDue(LocalDateTime.now().plusHours(1));
        given(memberRepository.findByPasswordResetKey(anyString()))
            .willReturn(Optional.of(member));
        //when
        String email = memberService.authPasswordResetKey("1");

        //then
        assertEquals(member.getEmail(), email);
        assertTrue(LocalDateTime.now().plusMinutes(1).isAfter(member.getPasswordResetDue()));
    }

    @Test
    @DisplayName("???????????? ????????? ????????? ?????? ?????? - ???????????? ?????? ???")
    void fail_resetAuth_NotValidUrl() {
        //given
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.authPasswordResetKey("1"));
        //then
        assertEquals(ErrorCode.DATA_NOT_VALID, exception.getError());
    }

    @Test
    @DisplayName("???????????? ????????? ????????? ?????? ?????? - ??? ?????? ??????")
    void fail_resetAuth_KeyExpired() {
        //given
        member.setPasswordResetDue(LocalDateTime.now().minusSeconds(1));
        given(memberRepository.findByPasswordResetKey(anyString()))
            .willReturn(Optional.of(member));
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.authPasswordResetKey("1"));
        //then
        assertEquals(ErrorCode.ACCESS_NOT_VALID, exception.getError());
    }

    @Test
    @DisplayName("???????????? ????????? ??????")
    void success_resetProcess() {
        //given
        member.setPasswordResetKey("key");
        member.setPassword("1111");
        given(memberRepository.findById(anyString()))
            .willReturn(Optional.of(member));
        //when
        memberService.processResetPassword("1", "1111", "key");
        //then
        assertNotEquals("1111", member.getPassword());
    }

    @Test
    @DisplayName("???????????? ????????? ?????? - ???????????? ?????? ??????")
    void fail_resetProcess_userNotFound() {
        //given
        //when
        CustomException exception = assertThrows(CustomException.class, () ->
            memberService.processResetPassword("1", "1111", "key"));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getError());
    }

}