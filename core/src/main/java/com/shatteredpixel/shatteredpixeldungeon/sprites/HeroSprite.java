package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeroDisguise;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

public class HeroSprite extends CharSprite {
	
	// 고해상도 도트 규격
	private static final int FRAME_WIDTH	= 48;  
	private static final int FRAME_HEIGHT	= 60;  
	
	private static final int RUN_FRAMERATE	= 20;
	
	
	
	private Animation fly;
	private Animation read;

	public HeroSprite() {
		super();
		
		// 튕김 방지: Dungeon.hero가 생성 전일 경우를 대비
		if (Dungeon.hero != null) {
			texture( Dungeon.hero.heroClass.spritesheet() );
			link( Dungeon.hero );
		} else {
			texture( Assets.Sprites.MAGE );
		}

		updateArmor();

		if (ch != null && ch.isAlive()) idle();
	}

	public void disguise(HeroClass cls){
		texture( cls.spritesheet() );
		updateArmor();
	}
	
	public void updateArmor() {
		// 텍스처 설정
		texture( (ch != null) ? ((Hero)ch).heroClass.spritesheet() : Assets.Sprites.MAGE );
		
		int tier = (Dungeon.hero != null) ? Dungeon.hero.tier() : 0;
// 현재 캐릭터의 텍스처를 직접 전달하여 정확한 크기의 필름을 생성합니다.
SmartTexture tx = TextureCache.get( (ch != null) ? ((Hero)ch).heroClass.spritesheet() : Assets.Sprites.MAGE );
TextureFilm film = new TextureFilm( tiers(tx), tier, FRAME_WIDTH, FRAME_HEIGHT );
		
		// [핵심] 게임 내 거대화 방지: 48x60 도트를 1/4로 축소하여 12x15 영역에 맞춤
		scale.set( 0.25f ); 
		
		// 애니메이션 데이터 정의
		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );
		
		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );
		
		die = new Animation( 20, false );
		die.frames( film, 8, 9, 10, 11, 12, 11 );
		
		attack = new Animation( 15, false );
		attack.frames( film, 13, 14, 15, 0 );
		
		zap = attack.clone();
		
		operate = new Animation( 8, false );
		operate.frames( film, 16, 17, 16, 17 );
		
		fly = new Animation( 1, true );
		fly.frames( film, 18 );

		read = new Animation( 20, false );
		read.frames( film, 19, 20, 20, 20, 20, 20, 20, 20, 20, 19 );
		
		if (ch != null) {
			if (ch.isAlive()) idle(); else die();
		}
	}
	
	@Override
	public void place( int p ) {
		super.place( p );
		if (Game.scene() instanceof GameScene) Camera.main.panFollow(this, 5f);
	}

	@Override
	public void move( int from, int to ) {
		super.move( from, to );
		if (ch != null && ch.flying) {
			play( fly );
		}
		Camera.main.panFollow(this, 20f);
	}

	@Override
	public void idle() {
		super.idle();
		if (ch != null && ch.flying) {
			play( fly );
		}
	}

	@Override
	public void jump( int from, int to, float height, float duration,  Callback callback ) {
		super.jump( from, to, height, duration, callback );
		play( fly );
		Camera.main.panFollow(this, 20f);
	}

	public synchronized void read() {
		animCallback = new Callback() {
			@Override
			public void call() {
				idle();
				if (ch != null) ch.onOperateComplete();
			}
		};
		play( read );
	}

	@Override
	public void bloodBurstA(PointF from, int damage) { 
		// 인간형 캐릭터에 대한 폭력성 등급 조절을 위해 비워둠
	}

	@Override
	public void update() {
		if (ch != null) sleeping = ch.isAlive() && ((Hero)ch).resting;
		super.update();
	}
	
public void sprint( float speed ) {
        if (run != null) run.delay = 1f / speed / RUN_FRAMERATE;
    }
    
// 인자 없는 버전 (다른 파일들 에러 방지용)
    public static TextureFilm tiers() {
        SmartTexture tx = TextureCache.get(Assets.Sprites.MAGE);
        return new TextureFilm( tx, tx.width, FRAME_HEIGHT );
    }

    // 인자 있는 버전 (현재 클래스 내부 사용용)
    public static TextureFilm tiers(SmartTexture texture) {
        return new TextureFilm( texture, texture.width, FRAME_HEIGHT );
    }

    // 2. 인게임 11시 UI용 (여기에 좌표 설정이 들어감)
    public static Image avatar( Hero hero ){
        HeroClass cl = (hero.buff(HeroDisguise.class) != null) ? 
            hero.buff(HeroDisguise.class).getDisguise() : hero.heroClass;

        Image img = avatar(cl, hero.tier());
        
        // origin을 0으로 잡고 x, y를 0으로 초기화해서 화면에 보이게 함
        img.origin.set(0, 0); 
        img.x = 0; 
        img.y = 0; 
        
        return img;
    }

    // 3. 실제 이미지를 잘라오는 로직
    public static Image avatar( HeroClass cl, int armorTier ) {
        Image avatar = new Image( cl.spritesheet() );
        
        // 48x60 규격에 맞게 프레임 절단
        avatar.frame( 0, armorTier * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT );
        
        // 1/4 축소
        avatar.origin.set( FRAME_WIDTH / 2f, FRAME_HEIGHT / 2f );
        avatar.scale.set( 0.25f ); 
        
        return avatar;
    }
} // 클래스 끝