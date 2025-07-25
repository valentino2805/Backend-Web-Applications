package pe.upc.cleanview.backend.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upc.cleanview.backend.iam.domain.services.UserCommandService;
import pe.upc.cleanview.backend.iam.interfaces.rest.resources.AuthenticatedUserResource;
import pe.upc.cleanview.backend.iam.interfaces.rest.resources.SignInResource;
import pe.upc.cleanview.backend.iam.interfaces.rest.resources.SignUpResource;
import pe.upc.cleanview.backend.iam.interfaces.rest.resources.UserResource;
import pe.upc.cleanview.backend.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import pe.upc.cleanview.backend.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import pe.upc.cleanview.backend.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import pe.upc.cleanview.backend.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import pe.upc.cleanview.backend.profiles.domain.model.commands.CreateProfileCommand;
import pe.upc.cleanview.backend.profiles.domain.model.valueobjects.UserId;
import pe.upc.cleanview.backend.profiles.domain.services.ProfileCommandService;

/**
 * AuthenticationController
 * <p>
 *     This controller is responsible for handling authentication requests.
 *     It exposes two endpoints:
 *     <ul>
 *         <li>POST /api/v1/auth/sign-in</li>
 *         <li>POST /api/v1/auth/sign-up</li>
 *     </ul>
 * </p>
 */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication Endpoints")
public class AuthenticationController {

  private final UserCommandService userCommandService;
  private final ProfileCommandService profileCommandService;

  public AuthenticationController(UserCommandService userCommandService, ProfileCommandService profileCommandService) {
    this.userCommandService = userCommandService;
    this.profileCommandService = profileCommandService;
  }

  /**
   * Handles the sign-in request.
   * @param signInResource the sign-in request body.
   * @return the authenticated user resource.
   */
  @PostMapping("/sign-in")
  public ResponseEntity<AuthenticatedUserResource> signIn(
      @RequestBody SignInResource signInResource) {

    var signInCommand = SignInCommandFromResourceAssembler
        .toCommandFromResource(signInResource);
    var authenticatedUser = userCommandService.handle(signInCommand);
    if (authenticatedUser.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var authenticatedUserResource = AuthenticatedUserResourceFromEntityAssembler
        .toResourceFromEntity(
            authenticatedUser.get().getLeft(), authenticatedUser.get().getRight());
    return ResponseEntity.ok(authenticatedUserResource);
  }

  /**
   * Handles the sign-up request.
   * @param signUpResource the sign-up request body.
   * @return the created user resource.
   */
  @PostMapping("/sign-up")
  public ResponseEntity<UserResource> signUp(@RequestBody SignUpResource signUpResource) {
    var signUpCommand = SignUpCommandFromResourceAssembler
        .toCommandFromResource(signUpResource);
    var user = userCommandService.handle(signUpCommand);
    if (user.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    var createdUser = user.get();
    var profileCommand = new CreateProfileCommand(
            createdUser.getUsername(),
            createdUser.getPassword(),
            new UserId(createdUser.getId())
    );
    profileCommandService.handle(profileCommand);

    var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
    return new ResponseEntity<>(userResource, HttpStatus.CREATED);
  }
}
