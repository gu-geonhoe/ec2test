package com.example.Api.main;


import com.example.Api.category.Category;
import com.example.Api.category.CategoryService;
import com.example.Api.member.Member;
import com.example.Api.member.MemberService;
import com.example.Api.product.Product;
import com.example.Api.product.ProductHeartService;
import com.example.Api.product.ProductService;
import com.example.Api.review.Review;
import com.example.Api.review.ReviewHeartService;
import com.example.Api.review.ReviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Tag(name = "Main Page", description = "Main Page API")
@Api(tags = "Main Page")

@RestController
@RequestMapping("/main")
@Validated
public class MainController {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final MemberService memberService;
    private final ReviewHeartService reviewHeartService;

    private final ProductHeartService productHeartService;
    private final CategoryService categoryService;

    public MainController(ProductService productService, ReviewService reviewService,
                          MemberService memberService, CategoryService categoryService,
                          ProductHeartService productHeartService,
                          ReviewHeartService reviewHeartService) {
        this.productService = productService;
        this.reviewService = reviewService;
        this.memberService = memberService;
        this.categoryService = categoryService;
        this.productHeartService = productHeartService;
        this.reviewHeartService = reviewHeartService;
    }

    @ApiOperation(value = "?????? ????????? ??????",
            notes = "??? ?????? ?????? ??? top5, ?????? ??????, ????????? ?????? \n - \n " )
    @GetMapping
    public ResponseEntity getMain(HttpServletRequest request){

        //?????? top5 ????????? ??????
        String company = "all";
        List<Product> top5 = productService.getTop5Products(company);

        // ??????????????? ??????
        List<Review> bestReviews = reviewService.getTop5Reviews();

        boolean loginStatus = memberService.memberCheck(request);
        //?????? ?????? ??????????????? true,  ???????????? false  ??????

        // ?????? ?????? ??????
        int size = 12;
        List<Product> recommends = new ArrayList<>();

        if (loginStatus) {   //???????????? ???

            // ?????? ?????? ??????
            List<Category> allCategories = categoryService.findAllCategoryAsList();
            //????????? ????????? ?????? 1????????? ?????? ?????????????????? ????????? ?????????
            List<Category> atLeastOne = categoryService.checkAtLeastOneProduct(allCategories);
            recommends = productService.setRandomRecommendedProducts(atLeastOne,"main");

            if(top5!=null){
                checkHeartFlagsNotLongin(top5);
            }
            if(bestReviews!=null) {
                checkReviewHeartFlagsNotLongin(bestReviews);
            }
            if(recommends!=null) {
                checkHeartFlagsNotLongin(recommends);
            }

            return new ResponseEntity<>(
                    new MainResponseDto<>(top5,recommends, bestReviews),
                    HttpStatus.OK);
        }
        else {  // ????????? ???

            // ???????????? ??????
            Member member = memberService.getLoginMember();
            Category memberCategory = member.getCategory();
            if((memberCategory == null) || (memberCategory.getProducts().size()<size)){
                List<Category> allCategories = categoryService.findAllCategoryAsList();
                //????????? ????????? ?????? 1????????? ?????? ?????????????????? ????????? ?????????
                List<Category> atLeastOne = categoryService.checkAtLeastOneProduct(allCategories);
                if(memberCategory == null){
                    recommends = productService.setRandomRecommendedProducts(atLeastOne,"main");
                }
                else {
                    List<Product> products = productService.findProductsByCategory(memberCategory);
                    if(products.size() == 0){  // ??????????????? ???????????? ????????? ?????? ?????? ???
                        // ?????? ???????????? + ?????? ??????
                        recommends = productService.setRandomRecommendedProducts(atLeastOne,"main");
                    }
                    atLeastOne.removeIf(category1 -> category1 == memberCategory);
                    recommends = productService.setRecommendedProductsAtLeastOne(atLeastOne,"main",products);
                }
            }
            else {
                recommends = productService.setRecommendedProducts(memberCategory,"main");
            }

            if(top5!=null){
                checkHeartFlagsLogin(member,top5);
            }
            if(bestReviews!=null) {
                checkReviewHeartFlagsLogin(member,bestReviews);
            }
            if(recommends!=null) {
                checkHeartFlagsLogin(member,recommends);
            }

            return new ResponseEntity<>(
                    new MainResponseDto<>(top5,recommends, bestReviews),
                    HttpStatus.OK);
        }

    }

    public void checkHeartFlagsNotLongin(List<Product> products){
        if(!products.isEmpty()){
            for(Product product : products){
                product.setHeartFlag(false); // ????????? ?????? OFF
            }
        }

    }
    public void checkHeartFlagsLogin(Member member, List<Product> products){
        if(!products.isEmpty()){
            for(Product product : products){
                //?????? ???????????? false, ????????? ???????????? true
                product.setHeartFlag(!productHeartService.checkAlreadyHeart(member, product));
            }
        }

    }
    public void checkReviewHeartFlagsNotLongin(List<Review> reviews){
        if(!reviews.isEmpty()){
            for(Review review : reviews){
                review.setReviewHeartFlag(false); // ????????? ?????? OFF
            }
        }
    }
    public void checkReviewHeartFlagsLogin(Member member, List<Review> reviews){
        if(!reviews.isEmpty()){
            for(Review review : reviews){
                if(reviewHeartService.checkAlreadyHeart(member,review)){
                    //?????? ???????????? false, ????????? ???????????? true
                    review.setReviewHeartFlag(false);
                }
                else{
                    review.setReviewHeartFlag(true);
                }
            }
        }
    }
}
