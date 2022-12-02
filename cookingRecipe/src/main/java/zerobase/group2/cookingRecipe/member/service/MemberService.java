package zerobase.group2.cookingRecipe.member.service;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zerobase.group2.cookingRecipe.member.component.MailComponent;
import zerobase.group2.cookingRecipe.member.dto.EditMemberInfo;
import zerobase.group2.cookingRecipe.member.dto.EditPassword;
import zerobase.group2.cookingRecipe.member.dto.MemberDto;
import zerobase.group2.cookingRecipe.member.dto.MemberRegister.Request;
import zerobase.group2.cookingRecipe.member.entity.Member;
import zerobase.group2.cookingRecipe.member.exception.MemberException;
import zerobase.group2.cookingRecipe.member.repository.MemberRepository;
import zerobase.group2.cookingRecipe.member.type.MemberStatus;
import zerobase.group2.cookingRecipe.type.ErrorCode;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {
    private final MemberRepository memberRepository;
    private final MailComponent mailComponent;

    public MemberDto register(Request request) {
        memberRepository.findById(request.getEmail())
            .ifPresent(e -> {
                throw new MemberException(ErrorCode.EMAIL_ALREADY_REGISTERED);
            });

        String uuid = UUID.randomUUID().toString();

        Member member = memberRepository.save(Member.builder()
            .email(request.getEmail())
            .name(request.getName())
            .password(hashedPassword(request.getPassword()))
            .emailAuthDue(LocalDateTime.now().plusHours(1))
            .emailAuthKey(uuid)
            .emailAuthYn(false)
            .status(MemberStatus.BEFORE_AUTH)
            .build());

        sendEmail(request.getEmail(), uuid);

        return MemberDto.from(member);
    }

    private void sendEmail(String email, String uuid) {
        String subject = "EZ Cooking Recipe의 회원이 되신 것을 축하드립니다.";
        String text = "<p>아래 링크를 클릭해서 이메일 인증을 완료하세요.</p>" +
            "<div><a target='_blank' href='http://localhost:8080/member/email-auth?key=" +
            uuid + "'> 이메일 인증 </a></div>";
        mailComponent.sendMail(email, subject, text);
    }

    private String hashedPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean emailAuth(String key) {
        Member member = memberRepository.findByEmailAuthKey(key)
            .orElseThrow(() -> new MemberException(ErrorCode.DATA_NOT_VALID));

        if (member.isEmailAuthYn()) {
            throw new MemberException(ErrorCode.ACCESS_NOT_VALID);
        }

        if (LocalDateTime.now().isAfter(member.getEmailAuthDue())){
            throw new MemberException(ErrorCode.ACCESS_NOT_VALID);
        }

        member.setStatus(MemberStatus.IN_USE);
        member.setEmailAuthYn(true);
        member.setEmailAuthDue(LocalDateTime.now());
        memberRepository.save(member);

        return member.isEmailAuthYn();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = getMemberById(email);

//        logInValidate(member);

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

//        if (member.isAdminYn()) {
//            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
//        }

        return new User(member.getEmail(), member.getPassword(), grantedAuthorities);
    }

    public MemberDto getInfoById(String email) {
        return MemberDto.from(getMemberById(email));
    }

    private Member getMemberById(String email) {
        return memberRepository.findById(email)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보가 존재하지 않습니다."));
    }

    public MemberDto editMemberInfo(String email, EditMemberInfo.Request request) {
        Member member = getMemberById(email);
        member.setName(request.getName());
        memberRepository.save(member);
        return MemberDto.from(member);
    }

    public boolean editPassword(String email, EditPassword.Request request) {
        Member member = getMemberById(email);

        if(member.validatePassword(hashedPassword(request.getOldPassword()))){
            throw new MemberException(ErrorCode.DATA_NOT_VALID);
        }

        member.setPassword(hashedPassword(request.getNewPassword()));
        memberRepository.save(member);
        return true;
    }

    public boolean withdraw(String email, String password) {
        Member member = getMemberById(email);

        if(member.validatePassword(hashedPassword(password))){
            throw new MemberException(ErrorCode.DATA_NOT_VALID);
        }

        member.setName("탈퇴회원");
        member.setStatus(MemberStatus.WITHDRAW);
        memberRepository.save(member);
        return true;
    }

    public boolean sendResetEmail(String email) {
        Member member = getMemberById(email);

        member.setEmailAuthKey(UUID.randomUUID().toString());
        member.setEmailAuthDue(LocalDateTime.now().plusMinutes(10));
        memberRepository.save(member);

        sendResetEmail(member.getEmail(), member.getEmailAuthKey());

        return true;
    }

    private void sendResetEmail(String email, String uuid) {
        String subject = "비밀번호 초기화 이메일";
        String text = "<p>아래 링크를 클릭해서 비밀번호를 재설정하세요.</p>" +
            "<div><a target='_blank' href='http://localhost:8080/member/reset-password?key=" +
            uuid + "'> 비밀번호 초기화 </a></div>";
        mailComponent.sendMail(email, subject, text);
    }

    public String resetAuth(String key) {
        Member member = memberRepository.findByPasswordAuthKey(key)
            .orElseThrow(() -> new MemberException(ErrorCode.DATA_NOT_VALID));

        if (LocalDateTime.now().isAfter(member.getPasswordAuthDue())){
            throw new MemberException(ErrorCode.ACCESS_NOT_VALID);
        }

        member.setPasswordAuthKey("");
        member.setPasswordAuthDue(LocalDateTime.now());
        memberRepository.save(member);

        return member.getEmail();
    }

    public boolean resetProcess(String email, String password) {
        Member member = getMemberById(email);

        member.setPassword(hashedPassword(password));
        memberRepository.save(member);

        return true;
    }
}
