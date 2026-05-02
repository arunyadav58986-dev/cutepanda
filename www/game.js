const UnityAds = window.Capacitor?.Plugins?.UnityAds;

const GAME_ID = "6104806";
const REWARDED_PLACEMENT_ID = "Rewarded_Android";

let adsReady = false;

async function initAds() {
  if (!UnityAds) {
    console.log("UnityAds plugin not found");
    return;
  }

  try {
    await UnityAds.init({
      gameId: GAME_ID,
      testMode: true,
    });

    console.log("Unity Ads init OK");

    await loadRewardedAd();

  } catch (e) {
    console.log("Unity Ads init/load error:", e);
  }
}

async function loadRewardedAd() {
  if (!UnityAds) return;

  try {
    await UnityAds.loadRewarded({
      placementId: REWARDED_PLACEMENT_ID,
    });

    adsReady = true;
    console.log("Rewarded ad loaded");
  } catch (e) {
    adsReady = false;
    console.log("Rewarded ad load failed:", e);
  }
}

async function showRewardedAd() {
  if (!UnityAds || !adsReady) {
    console.log("Ad not ready");
    return false;
  }

  try {
    const result = await UnityAds.showRewarded({
      placementId: REWARDED_PLACEMENT_ID,
    });

    console.log("Rewarded ad shown:", result);

    adsReady = false;
    await loadRewardedAd();

    return true;
  } catch (e) {
    console.log("Rewarded ad show failed:", e);

    adsReady = false;
    await loadRewardedAd();

    return false;
  }
}

document.addEventListener("deviceready", () => {
  initAds();
});


kaplay({
    width: 500,
    height: 800,
    letterbox: true,
    stretch: true,
});

//document.addEventListener("deviceready", async () => {
  //  await initAds();
 //   await showBanner();
//});


// sprites
loadSprite("live", "heart-o.png");
loadSprite("player", "n3.png")
loadSprite("apple", "apple.png")
loadSprite("enemy", "lightning-o.png");

//  backgrounds 
loadSprite("bg", "10.png");
loadSprite("bg1" ,"9.png");
loadSprite("bg2", "8.png");
loadSprite("bg3", "11.png");
loadSprite("bg4" , "12.png");
// 

loadSprite("hit", "apple.png");
loadSprite("die", "skul.png");
// all sprites




scene("game", () => {

const bgs = ["bg","bg1", "bg2","bg3","bg4"];
let score = 0;
const randombg = choose(bgs);


add([
  sprite(randombg ,{ width:500, height:800 }),
  pos(0, 0),
  fixed(),
]);

const player = add([
  sprite("player"),
  pos(200,691),
  area(),
  body(),
  health(3),
])




player.onUpdate(() => {
  if (player.pos.x < 0){
    player.pos.x = 0
  }

  if (player.pos.x > 390){
    player.pos.x = 390
  }
})




onKeyDown("right", () => player.move(200,0))
onKeyDown("left", () => player.move(-200,0))


onUpdate(() => {
  if (isMouseDown()) {
    const tpos = mousePos();

    if (tpos.x < width() / 2) {
      player.move(-320,0);
    }

    else {
      player.move(320,0);
    }
  }
})



loop(rand(0.6,0.7), () => {
  const enemy = add([
    sprite("enemy"),
    pos(rand(1,450),0),
    area(),
    offscreen({ destroy: true }),
    "enemy",
  ])

  enemy.onUpdate(() => {
    enemy.move(0,400)
  })
})


loop(rand(0.7,0.9), () => {
  const apple = add([
    sprite("apple"),
    pos(rand(0,454) ,0),
    offscreen( { destroy: true }),
    area(),
    "apple",
  ])

  apple.onUpdate(() => {
    apple.move(0,300)
  })
})

loop(rand(13,19), () => {
  const live = add([
    sprite("live"),
    pos(rand(0,454),0),
    offscreen( {destroy: true}),
    area(),
    "live",
  ])

  live.onUpdate(() => {
    live.move(0,300)
  })
})

function updateHP() {

  destroyAll("heart-icon");
  
  for (let i = 0; i < player.hp(); i++) {
    add([
          sprite("live"),
          pos(30 + i * 40, 30),
          scale(0.9), 
          fixed(), 
          "heart-icon",
        ])
  }
 
}





const scoreLabel = add([
    text(score),
     pos(410,9),
    fixed(),
]);


player.onCollide("enemy", (enemy) => {
    player.hurt(1); 
    destroy(enemy);
    updateHP();
    shake(5);
})

player.onCollide("apple", (apple) => {
    destroy(apple);  
    score += 1;      
    scoreLabel.text = score;
});

player.onCollide("live", (live) => {
  destroy(live);
  player.heal(1);  
  updateHP();
})



player.onDeath(() => {


  destroy(player);

  add([
    sprite("die"),
    pos(center()),
    anchor("center"),
    z(10),
  ])


const btn = add([
    rect(200, 60, { radius: 8 }), 
    pos(center().add(0, 100)), 
    color(50, 50, 50),            
    area(),                       
    anchor("center"),             
    outline(4, rgb(255, 255, 255)),
]);

btn.add([
    text("REPLAY", { size: 24 }),
    anchor("center",39),
    color(255, 255, 255),
]);








btn.onClick(async () => {
  await showRewardedAd();
  go("game");
});





});

});




go("game")
