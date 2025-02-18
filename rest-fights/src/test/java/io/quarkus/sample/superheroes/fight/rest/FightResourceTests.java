package io.quarkus.sample.superheroes.fight.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate.FightToNarrateLocation;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.service.FightService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

/**
 * Tests the resource layer ({@link FightResource} specifically).
 */
@QuarkusTest
public class FightResourceTests {
	private static final String DEFAULT_FIGHT_ID = new ObjectId().toString();
	private static final Instant DEFAULT_FIGHT_DATE = Instant.now();

	private static final String DEFAULT_HERO_NAME = "Super Baguette";
	private static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
	private static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
	private static final int DEFAULT_HERO_LEVEL = 42;
	private static final String HEROES_TEAM_NAME = "heroes";

	private static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
	private static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
	private static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
	private static final int DEFAULT_VILLAIN_LEVEL = 40;
	private static final String VILLAINS_TEAM_NAME = "villains";
  private static final String DEFAULT_NARRATION = """
                                                  This is a default narration - NOT a fallback!
                                                  
                                                  High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                                  With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                                  
                                                  In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                                  The people knew that their protector had once again ensured their safety.
                                                  """;

  private static final String DEFAULT_LOCATION_NAME = "Gotham City";
  private static final String DEFAULT_LOCATION_DESCRIPTION = "An American city rife with corruption and crime, the home of its iconic protector Batman.";
  private static final String DEFAULT_LOCATION_PICTURE = "gotham_city.png";

	@InjectMock
	FightService fightService;

	@BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Test
	public void helloEndpoint() {
		get("/api/fights/hello")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(TEXT)
			.body(is("Hello Fight Resource"));

		verifyNoInteractions(this.fightService);
	}

  @Test
  public void helloHeroesEndpoint() {
    when(this.fightService.helloHeroes())
      .thenReturn(Uni.createFrom().item("Hello Hero Resource"));

    get("/api/fights/hello/heroes")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Hero Resource"));

    verify(this.fightService).helloHeroes();
    verifyNoMoreInteractions(this.fightService);
  }

  @Test
  public void helloVillainsEndpoint() {
    when(this.fightService.helloVillains())
      .thenReturn(Uni.createFrom().item("Hello Villains Resource"));

    get("/api/fights/hello/villains")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Villains Resource"));

    verify(this.fightService).helloVillains();
    verifyNoMoreInteractions(this.fightService);
  }

  @Test
  public void helloNarrationEndpoint() {
    when(this.fightService.helloNarration())
      .thenReturn(Uni.createFrom().item("Hello Narration Resource"));

    get("/api/fights/hello/narration")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Narration Resource"));

    verify(this.fightService).helloNarration();
    verifyNoMoreInteractions(this.fightService);
  }

	@Test
	public void helloLocationEndpoint() {
		when(this.fightService.helloLocations())
			.thenReturn(Uni.createFrom().item("Hello FightLocation Resource"));

		get("/api/fights/hello/locations")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(TEXT)
			.body(is("Hello FightLocation Resource"));

		verify(this.fightService).helloLocations();
		verifyNoMoreInteractions(this.fightService);
	}

  @Test
  public void shouldGetNarration() {
    when(this.fightService.narrateFight(any(FightToNarrate.class)))
      .thenReturn(Uni.createFrom().item(DEFAULT_NARRATION));

    given()
      .log().all(true)
      .accept(TEXT)
      .contentType(JSON)
      .body(createFightToNarrateHeroWon())
      .when().post("/api/fights/narrate").then()
        .log().all(true)
        .statusCode(OK.getStatusCode())
        .contentType(TEXT)
        .body(is(DEFAULT_NARRATION));

    verify(this.fightService).narrateFight(any(FightToNarrate.class));
    verifyNoMoreInteractions(this.fightService);
  }

  // This is written as an @Test instead of @ParameterizedTest
  // because @MethodSource does not like java records for some reason
  @Test
  public void shouldNotGetNarrationBecauseInvalidFight() {
    invalidFightsToNarrate().forEach(fight -> {
      given()
        .accept(TEXT)
        .contentType(JSON)
        .body(fight)
        .when().post("/api/fights/narrate").then()
        .statusCode(BAD_REQUEST.getStatusCode());

      verifyNoInteractions(this.fightService);
    });
  }

  @Test
  public void shouldGetRandomLocation() {
    when(this.fightService.findRandomLocation())
      .thenReturn(Uni.createFrom().item(FightResourceTests::createDefaultLocation));

    var randomLocation = get("/api/fights/randomlocation")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(JSON)
      .extract().as(FightLocation.class);

    assertThat(randomLocation)
      .isNotNull().usingRecursiveComparison()
      .isEqualTo(createDefaultLocation());

    verify(this.fightService).findRandomLocation();
    verifyNoMoreInteractions(this.fightService);
  }

  @Test
	public void shouldGetRandomFighters() {
		when(this.fightService.findRandomFighters())
			.thenReturn(Uni.createFrom().item(createDefaultFighters()));

		var randomFighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(randomFighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createDefaultFighters());

		verify(this.fightService).findRandomFighters();
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetNoFights() {
		when(this.fightService.findAllFights())
      .thenReturn(Uni.createFrom().item(List.of()));

		get("/api/fights")
			.then()
			.statusCode(OK.getStatusCode())
			.body("$.size()", is(0));

		verify(this.fightService).findAllFights();
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetAllFights() {
		when(this.fightService.findAllFights())
			.thenReturn(Uni.createFrom().item(List.of(createFightHeroWon())));

		var fights = get("/api/fights")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().body()
      .jsonPath().getList(".", Fight.class);

      assertThat(fights)
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(createFightHeroWon());

		verify(this.fightService).findAllFights();
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetNoFightFound() {
		when(this.fightService.findFightById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().nullItem());

		get("/api/fights/{id}", DEFAULT_FIGHT_ID)
			.then().statusCode(NOT_FOUND.getStatusCode());

		verify(this.fightService).findFightById(eq(DEFAULT_FIGHT_ID));
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetFight() {
		when(this.fightService.findFightById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = get("/api/fights/{id}", DEFAULT_FIGHT_ID)
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fight.class);

    assertThat(fight)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		verify(this.fightService).findFightById(eq(DEFAULT_FIGHT_ID));
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldNotPerformFightNullFighters() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.post("/api/fights")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.fightService);
	}

	@Test
	public void shouldNotPerformFightInvalidFighters() {
    invalidFighters().forEach(fighters -> {
      given()
        .when()
          .contentType(JSON)
          .accept(JSON)
          .body(fighters)
          .post("/api/fights")
        .then()
          .statusCode(BAD_REQUEST.getStatusCode());

      verifyNoInteractions(this.fightService);
    });
	}

	@Test
	public void shouldPerformFight() {
		when(this.fightService.performFight(eq(createDefaultFightRequest())))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(createDefaultFightRequest())
				.post("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Fight.class);

    assertThat(fight)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		verify(this.fightService).performFight(eq(createDefaultFightRequest()));
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldPingOpenAPI() {
		get("/q/openapi")
			.then().statusCode(OK.getStatusCode());
	}

	private static Stream<Fighters> invalidFighters() {
		return Stream.of(
      new Fighters(),
			new Fighters(createDefaultHero(), null),
			new Fighters(null, createDefaultVillain()),
			new Fighters(new Hero(null, DEFAULT_HERO_LEVEL, DEFAULT_HERO_PICTURE, DEFAULT_HERO_POWERS), createDefaultVillain()),
			new Fighters(new Hero("", DEFAULT_HERO_LEVEL, DEFAULT_HERO_PICTURE, DEFAULT_HERO_POWERS), createDefaultVillain()),
			new Fighters(new Hero(DEFAULT_HERO_NAME, DEFAULT_HERO_LEVEL, "", DEFAULT_HERO_POWERS), createDefaultVillain()),
			new Fighters(createDefaultHero(), new Villain(null, DEFAULT_VILLAIN_LEVEL, DEFAULT_VILLAIN_PICTURE, DEFAULT_VILLAIN_POWERS)),
			new Fighters(createDefaultHero(), new Villain("", DEFAULT_VILLAIN_LEVEL, DEFAULT_VILLAIN_PICTURE, DEFAULT_VILLAIN_POWERS)),
			new Fighters(createDefaultHero(), new Villain(DEFAULT_VILLAIN_NAME, DEFAULT_VILLAIN_LEVEL, "", DEFAULT_VILLAIN_POWERS))
		);
	}

  private static Stream<FightToNarrate> invalidFightsToNarrate() {
    return Stream.of(
      new FightToNarrate(null, null, null, 0, null, null, null, 0, null),
      new FightToNarrate("", "", "", 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, "", "", 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, "", 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, DEFAULT_VILLAIN_NAME, "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, "", "", 0, null, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, null, 0, null, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, null, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, DEFAULT_VILLAIN_NAME, null, 0, null)
    );
  }

	private static Hero createDefaultHero() {
		return new Hero(
			DEFAULT_HERO_NAME,
			DEFAULT_HERO_LEVEL,
			DEFAULT_HERO_PICTURE,
			DEFAULT_HERO_POWERS
		);
	}

	private static Villain createDefaultVillain() {
		return new Villain(
			DEFAULT_VILLAIN_NAME,
			DEFAULT_VILLAIN_LEVEL,
			DEFAULT_VILLAIN_PICTURE,
			DEFAULT_VILLAIN_POWERS
		);
	}

	private static Fighters createDefaultFighters() {
		return new Fighters(createDefaultHero(), createDefaultVillain());
	}

  private static FightRequest createDefaultFightRequest() {
    return new FightRequest(createDefaultHero(), createDefaultVillain(), createDefaultLocation());
  }

	private static Fight createFightHeroWon() {
		var fight = new Fight();
		fight.id = new ObjectId(DEFAULT_FIGHT_ID);
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_HERO_NAME;
		fight.winnerLevel = DEFAULT_HERO_LEVEL;
		fight.winnerPicture = DEFAULT_HERO_PICTURE;
    fight.winnerPowers = DEFAULT_HERO_POWERS;
		fight.loserName = DEFAULT_VILLAIN_NAME;
		fight.loserLevel = DEFAULT_VILLAIN_LEVEL;
		fight.loserPicture = DEFAULT_VILLAIN_PICTURE;
    fight.loserPowers = DEFAULT_VILLAIN_POWERS;
		fight.winnerTeam = HEROES_TEAM_NAME;
		fight.loserTeam = VILLAINS_TEAM_NAME;
    fight.location = createDefaultLocation();

		return fight;
	}

  private static FightToNarrate createFightToNarrateHeroWon() {
    return new FightToNarrate(
      HEROES_TEAM_NAME,
      DEFAULT_HERO_NAME,
      DEFAULT_HERO_POWERS,
      DEFAULT_HERO_LEVEL,
      VILLAINS_TEAM_NAME,
      DEFAULT_VILLAIN_NAME,
      DEFAULT_VILLAIN_POWERS,
      DEFAULT_VILLAIN_LEVEL,
      new FightToNarrateLocation(createDefaultLocation())
    );
	}

  private static FightLocation createDefaultLocation() {
    return new FightLocation(DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_DESCRIPTION, DEFAULT_LOCATION_PICTURE);
  }
}
