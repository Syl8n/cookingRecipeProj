package zerobase.group2.cookingRecipe.recipe.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import zerobase.group2.cookingRecipe.comment.dto.CommentDto;
import zerobase.group2.cookingRecipe.common.model.ResponseResult;
import zerobase.group2.cookingRecipe.recipe.dto.RecipeInput;
import zerobase.group2.cookingRecipe.recipe.dto.SearchCondition;
import zerobase.group2.cookingRecipe.recipe.service.RecipeService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/recipe")
@Api(tags = {"레시피 관련 api"})
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping("/create")
    @ApiOperation("레시피를 추가합니다")
    public ResponseResult createRecipe(
            @RequestBody @Valid RecipeInput.Request request, Principal principal) {
        return ResponseResult.ok(
                recipeService.createRecipe(
                        request.getTitle(),
                        request.getMainImagePathSmall(),
                        request.getMainImagePathBig(),
                        request.getType1(),
                        request.getType2(),
                        request.getIngredients(),
                        request.getKcal(),
                        request.getManual(),
                        request.getManualImagePath(),
                        principal.getName()
                )
        );
    }

    @GetMapping("/read/{recipeId}")
    @ApiOperation("레시피의 상세 정보를 조회합니다")
    public ResponseResult readRecipe(@PathVariable
                                     @ApiParam("주소창에 보이는 문자형 ID")
                                     String recipeId,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        return ResponseResult.ok(
                recipeService.readRecipe(recipeId, request.getCookies(),
                        response));
    }

    @GetMapping("/edit/{recipeId}")
    @ApiOperation("레시피의 수정권한이 있는지 확인합니다")
    public ResponseResult checkAuthorityToEditRecipe(@PathVariable
                                                     @ApiParam("주소창에 보이는 문자형 ID")
                                                     String recipeId,
                                                     Principal principal) {
        return ResponseResult.ok(
                recipeService.checkAuthorityToEditRecipe(recipeId,
                        principal.getName()));
    }

    @PutMapping("/edit")
    @ApiOperation("레시피를 수정합니다")
    public ResponseResult processEditRecipe(
            @RequestBody RecipeInput.Request request,
            Principal principal) {
        return ResponseResult.ok(
                recipeService.processEditRecipe(
                        request.getId(),
                        request.getTitle(),
                        request.getMainImagePathSmall(),
                        request.getMainImagePathBig(),
                        request.getType1(),
                        request.getType2(),
                        request.getIngredients(),
                        request.getKcal(),
                        request.getManual(),
                        request.getManualImagePath(),
                        principal.getName()
                )
        );
    }

    @DeleteMapping("/delete/{recipeId}")
    @ApiOperation("레시피를 삭제합니다")
    public ResponseResult deleteRecipe(@PathVariable
                                       @ApiParam("주소창에 보이는 문자형 ID")
                                       String recipeId,
                                       Principal principal) {
        return ResponseResult.ok(
                recipeService.deleteRecipe(recipeId, principal.getName()));
    }

    @GetMapping("/find")
    @ApiOperation("제목, 조리방법, 요리분류에 기반해 레시피를 검색합니다")
    public ResponseResult findRecipes(@ModelAttribute
                                      SearchCondition searchCondition,
                                      Pageable pageable) {
        return ResponseResult.ok(
                recipeService.findRecipesByQuery(searchCondition.getTitle(),
                        searchCondition.getType1(), searchCondition.getType2(),
                        pageable));
    }

    @GetMapping("/comments/{visualId}")
    @ApiOperation("특정 레시피의 댓글 정보를 받아옵니다")
    public ResponseResult getComments(@PathVariable
                                      @ApiParam("주소창에 보이는 문자형 ID")
                                      String visualId) {
        List<CommentDto> comments = recipeService.getComments(visualId);
        return ResponseResult.ok(comments);
    }
}
