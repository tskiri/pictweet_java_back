package in.tech_camp.pictweet.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.tech_camp.pictweet.custom_user.CustomUserDetail;
import in.tech_camp.pictweet.entity.TweetEntity;
import in.tech_camp.pictweet.form.CommentForm;
import in.tech_camp.pictweet.form.SearchForm;
import in.tech_camp.pictweet.form.TweetForm;
import in.tech_camp.pictweet.repository.TweetRepository;
import in.tech_camp.pictweet.repository.UserRepository;
import in.tech_camp.pictweet.validation.ValidationOrder;
import lombok.AllArgsConstructor;


// REST用のController
@RestController
// URLの共通部分を記述。今回はツイート機能に関するAPIへのリクエストであることを示す。
@RequestMapping("/api/tweets")
@AllArgsConstructor
public class TweetController {
  private final TweetRepository tweetRepository;

  private final UserRepository userRepository;
  
  @PostMapping("/tweets")
  public String createTweet(@ModelAttribute("tweetForm") @Validated(ValidationOrder.class) TweetForm tweetForm,
                            BindingResult result, 
                            @AuthenticationPrincipal CustomUserDetail currentUser,
                            Model model) {

    if (result.hasErrors()) {
      List<String> errorMessages = result.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .collect(Collectors.toList());
      model.addAttribute("errorMessages", errorMessages);
      model.addAttribute("tweetForm", tweetForm);
      return "tweets/new";
    }
    return "redirect:/";
  }
    


  @PostMapping("/")
  public ResponseEntity<?> createTweet(@RequestBody @Validated(ValidationOrder.class) TweetForm tweetForm,
                            BindingResult result, 
                            @AuthenticationPrincipal CustomUserDetail currentUser) {
    if (result.hasErrors()) {
      List<String> errorMessages = result.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .collect(Collectors.toList());
      return ResponseEntity.badRequest().body(Map.of("messages", errorMessages));
    }

    TweetEntity tweet = new TweetEntity();
    tweet.setUser(userRepository.findById(currentUser.getId()));
    tweet.setText(tweetForm.getText());
    tweet.setImage(tweetForm.getImage());
      
    try {
      tweetRepository.insert(tweet);
      return ResponseEntity.ok().body(tweet);
    } catch (Exception e) {
      System.out.println("エラー：" + e);
      return ResponseEntity.internalServerError().body(Map.of("messages", List.of("Internal Server Error")));
    }
  }

  @PostMapping("/tweets/{tweetId}/delete")
  public String deleteTweet(@PathVariable("tweetId") Integer tweetId) {
    try {
      tweetRepository.deleteById(tweetId);
    } catch (Exception e) {
      System.out.println("エラー：" + e);
      return "redirect:/";
    }
    return "redirect:/";
  }

  @GetMapping("/tweets/{tweetId}/edit")
  public String editTweet(@PathVariable("tweetId") Integer tweetId, Model model) {
    TweetEntity tweet = tweetRepository.findById(tweetId);

    TweetForm tweetForm = new TweetForm();
    tweetForm.setText(tweet.getText());
    tweetForm.setImage(tweet.getImage());

    model.addAttribute("tweetForm", tweetForm);
    model.addAttribute("tweetId", tweetId);
    return "tweets/edit";
  }

  @PostMapping("/tweets/{tweetId}/update")
  public String updateTweet(@ModelAttribute("tweetForm") @Validated(ValidationOrder.class) TweetForm tweetForm,
                            BindingResult result,
                            @PathVariable("tweetId") Integer tweetId,
                            Model model) {

    if (result.hasErrors()) {
      List<String> errorMessages = result.getAllErrors().stream()
              .map(DefaultMessageSourceResolvable::getDefaultMessage)
              .collect(Collectors.toList());
      model.addAttribute("errorMessages", errorMessages);

      model.addAttribute("tweetForm", tweetForm);
      model.addAttribute("tweetId", tweetId);
      return "tweets/edit";
    }

    TweetEntity tweet = tweetRepository.findById(tweetId);
    tweet.setText(tweetForm.getText());
    tweet.setImage(tweetForm.getImage());

    try {
      tweetRepository.update(tweet);
    } catch (Exception e) {
      System.out.println("エラー：" + e);
      return "redirect:/";
    }

    return "redirect:/";
  }

  @GetMapping("/tweets/{tweetId}")
  public String showTweetDetail(@PathVariable("tweetId") Integer tweetId, Model model) {
      TweetEntity tweet = tweetRepository.findById(tweetId);
      CommentForm commentForm = new CommentForm();
      model.addAttribute("tweet", tweet);
      model.addAttribute("commentForm", commentForm);
      model.addAttribute("comments",tweet.getComments());
      return "tweets/detail";
  }

  @GetMapping("/tweets/search")
  public String searchTweets(@ModelAttribute("searchForm") SearchForm searchForm, Model model) {
    List<TweetEntity> tweets = tweetRepository.findByTextContaining(searchForm.getText());
    model.addAttribute("tweets", tweets);
    model.addAttribute("searchForm", searchForm);
    return "tweets/search";
  }
}
