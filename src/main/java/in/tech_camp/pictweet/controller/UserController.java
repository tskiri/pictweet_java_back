package in.tech_camp.pictweet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.tech_camp.pictweet.entity.TweetEntity;
import in.tech_camp.pictweet.entity.UserEntity;
import in.tech_camp.pictweet.form.UserForm;
import in.tech_camp.pictweet.repository.UserRepository;
import in.tech_camp.pictweet.service.UserService;
import in.tech_camp.pictweet.validation.ValidationOrder;
import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

  private final UserRepository userRepository;

  private final UserService userService;

  @PostMapping("/")
  // @RequestBody：リクエストの含まれるフォームデータなどを受け取り、Javaのオブジェクトに変換することができるアノテーション
  // ResponseEntityクラス：処理の成功や失敗、エラー発生などに応じたレスポンスデータを作成。
  // 処理の結果によって生成されるオブジェクト（戻り値）の型が変わるので、クラス定義の最初の部分で<?>を付ける。
  public ResponseEntity<?> createUser(@RequestBody @Validated(ValidationOrder.class) UserForm userForm, 
                                      BindingResult result) {
    userForm.validatePasswordConfirmation(result);
    if (userRepository.existsByEmail(userForm.getEmail())) {
      result.rejectValue("email", "null", "Email already exists");
    }

    // エラーがあるとき
    if (result.hasErrors()) {
      List<String> errorMessages = result.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .collect(Collectors.toList());
      return ResponseEntity.badRequest().body(Map.of("messages", errorMessages));
    }

    // エラーがなければエンティティに格納し、UserService経由でDB保存
    UserEntity userEntity = new UserEntity();
    userEntity.setNickname(userForm.getNickname());
    userEntity.setEmail(userForm.getEmail());
    userEntity.setPassword(userForm.getPassword());
    // DB保存が成功したらResponseEntity.ok()をレスポンスとして返す。
    try {
      userService.createUserWithEncryptedPassword(userEntity);
      return ResponseEntity.ok().body(Map.of(
        "id", userEntity.getId(),
        "nickname", userEntity.getNickname()
    ));
    // DB保存が失敗したらResponseEntity.internalServerError()をレスポンスとし、"Internal Server Error"もメッセージのとして含める。
    } catch (Exception e) {
      System.out.println("エラー：" + e);
      return ResponseEntity.internalServerError().body(Map.of("messages", List.of("Internal Server Error")));
    }
  }

  @GetMapping("/users/{userId}")
  public String showMypage(@PathVariable("userId") Integer userId, Model model) {
    UserEntity user = userRepository.findById(userId);
    List<TweetEntity> tweets = user.getTweets();

    model.addAttribute("nickname", user.getNickname());
    model.addAttribute("tweets", tweets);
    return "users/mypage";
  }
}
