package com.haymfilip.urbanstrike;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class GameView extends View {
    static class Entity { float x,y, r; int hp; boolean alive=true; int faction; Entity(float x,float y,int faction){this.x=x;this.y=y;this.r=16;this.hp=100;this.faction=faction;} }
    static class Weapon { String name; float damage, cooldown, range; int ammo, maxAmmo; boolean explosive;
        Weapon(String n,float d,float c,float rg,int a,boolean ex){name=n;damage=d;cooldown=c;range=rg;ammo=a;maxAmmo=a;explosive=ex;}
    }
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    final ArrayList<RectF> buildings = new ArrayList<>(); final ArrayList<Entity> enemies = new ArrayList<>(); final ArrayList<Weapon> weapons = new ArrayList<>();
    float px=900, py=900, angle=0, joyX=0, joyY=0; boolean joyActive=false; long lastShot=0; int weapon=0, score=0; float shake=0; boolean gameOver=false;
    float mapScale=0.42f; float tx=0, ty=0;
    final Random rnd = new Random(7);
    public GameView(Context c){ super(c); setFocusable(true); text.setTypeface(Typeface.create("sans",Typeface.BOLD)); init(); }
    void init(){
        // 20 urban blocks: 10 per fictional enemy sector.
        for(int row=0; row<5; row++) for(int col=0; col<4; col++){
            float x=220+col*420, y=180+row*320; buildings.add(new RectF(x,y,x+260,y+180));
        }
        int[] factions={0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1};
        for(int i=0;i<20;i++){
            float x=1400+rnd.nextInt(700), y=240+rnd.nextInt(1300); enemies.add(new Entity(x,y,factions[i]));
        }
        String[] pistols={"Sidearm A","Sidearm B","Sidearm C","Sidearm D","Sidearm E"};
        for(int i=0;i<5;i++) weapons.add(new Weapon(pistols[i],24+i*3,260-i*18,520,15,false));
        String[] rifles={"Rifle R1","Rifle R2","Rifle R3","Rifle R4","Rifle R5","Rifle R6"};
        for(int i=0;i<6;i++) weapons.add(new Weapon(rifles[i],34+i*3,145-i*7,760,30,false));
        weapons.add(new Weapon("Stun Grenade",5,950,420,3,true));
        weapons.add(new Weapon("Blast Grenade",75,950,480,3,true));
        weapons.add(new Weapon("Launcher",100,1200,900,5,true));
        weapons.add(new Weapon("MG M1",30,90,700,80,false));
        weapons.add(new Weapon("MG M2",38,110,760,80,false));
        weapons.add(new Weapon("MG M3",46,130,820,60,false));
    }
    @Override protected void onDraw(Canvas c){ super.onDraw(c); float w=getWidth(),h=getHeight();
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(18,22,25)); c.drawRect(0,0,w,h,p);
        tx=w/2f-px*mapScale; ty=h/2f-py*mapScale; if(gameOver){drawWorld(c,w,h); drawOverlay(c,w,h); return;}
        drawWorld(c,w,h); drawHud(c,w,h); invalidate();
        long now=System.currentTimeMillis();
        if(now-lastShot>80) updateEnemies(now);
    }
    void drawWorld(Canvas c,float w,float h){
        p.setColor(Color.rgb(46,49,52)); c.drawRect(0,0,w,h,p);
        p.setColor(Color.rgb(62,65,68)); for(int i=-4;i<10;i++){float gx=(i*210+tx%210); c.drawRect(gx,0,gx+3,h,p);} for(int i=-4;i<10;i++){float gy=(i*210+ty%210); c.drawRect(0,gy,w,gy+3,p);}
        for(int i=0;i<buildings.size();i++){ RectF b=buildings.get(i); RectF s=new RectF(b.left*mapScale+tx,b.top*mapScale+ty,b.right*mapScale+tx,b.bottom*mapScale+ty); p.setColor(Color.rgb(86+(i%2)*14,78,70)); c.drawRoundRect(s,10,10,p); p.setColor(Color.rgb(35,35,36)); for(float xx=s.left+20;xx<s.right-10;xx+=55) for(float yy=s.top+18;yy<s.bottom-8;yy+=46)c.drawRect(xx,yy,xx+25,yy+18,p); }
        for(Entity e:enemies) if(e.alive){float ex=e.x*mapScale+tx, ey=e.y*mapScale+ty; p.setColor(e.faction==0?Color.rgb(185,55,55):Color.rgb(185,120,45)); c.drawCircle(ex,ey,15,p); p.setColor(Color.WHITE); c.drawCircle(ex,ey,5,p); p.setColor(Color.rgb(22,22,22)); c.drawRect(ex-18,ey-28,ex+18,ey-23,p); p.setColor(Color.rgb(70,210,90)); c.drawRect(ex-18,ey-28,ex-18+36*(e.hp/100f),ey-23,p); }
        float cx=w/2f,cy=h/2f; p.setColor(Color.WHITE); p.setStrokeWidth(3); c.drawLine(cx-18,cy,cx+18,cy,p); c.drawLine(cx,cy-18,cx,cy+18,p);
        p.setColor(Color.rgb(45,65,45)); Path pl=new Path(); pl.moveTo(cx,cy-35); pl.lineTo(cx-22,cy+25); pl.lineTo(cx+22,cy+25); pl.close(); c.drawPath(pl,p);
    }
    void drawHud(Canvas c,float w,float h){
        // Top bar
        p.setColor(Color.argb(190,0,0,0)); c.drawRect(0,0,w,78,p);
        text.setTextSize(28); text.setColor(Color.WHITE); c.drawText("URBAN STRIKE",24,45,text); text.setTextSize(20); c.drawText("OBJECTIVE: CLEAR THE SECTOR",24,68,text);
        text.setTextSize(22); c.drawText("SCORE " + score, w-190,44,text);
        Weapon we=weapons.get(weapon); c.drawText(we.name+"  "+we.ammo+"/"+we.maxAmmo,w-360,69,text);
        // joystick
        p.setColor(Color.argb(90,255,255,255)); c.drawCircle(110,h-115,78,p); p.setColor(Color.argb(160,20,20,20)); c.drawCircle(110+joyX*45,h-115+joyY*45,38,p);
        // Fire button
        p.setColor(Color.argb(120,190,40,40)); c.drawCircle(w-115,h-120,68,p); text.setTextSize(24); text.setColor(Color.WHITE); c.drawText("FIRE",w-145,h-112,text);
        // weapon wheel buttons
        float bx=w-390, by=h-72; for(int i=0;i<weapons.size();i++){ if(i>14) break; float x=bx+(i%8)*48, y=by-(i/8)*46; p.setColor(i==weapon?Color.WHITE:Color.argb(140,0,0,0)); c.drawRoundRect(new RectF(x,y,x+42,y+38),8,8,p); text.setTextSize(14); text.setColor(i==weapon?Color.BLACK:Color.WHITE); c.drawText(""+(i+1),x+15,y+25,text); }
    }
    void drawOverlay(Canvas c,float w,float h){p.setColor(Color.argb(220,0,0,0));c.drawRect(0,0,w,h,p);text.setTextSize(64);text.setColor(Color.WHITE);c.drawText("SECTOR COMPLETE",w/2f-250,h/2f-30,text);text.setTextSize(28);c.drawText("Final score: "+score,w/2f-85,h/2f+25,text);}
    void updateEnemies(long now){
        for(Entity e:enemies) if(e.alive){float dx=px-e.x,dy=py-e.y,d=(float)Math.hypot(dx,dy); if(d<850){e.x+=(dx/d)*0.9f; e.y+=(dy/d)*0.9f;} if(d<95 && now%900<50){gameOver=true;} }
        boolean any=false; for(Entity e:enemies) if(e.alive){any=true;break;} if(!any)gameOver=true;
    }
    void shoot(){ long now=System.currentTimeMillis(); Weapon we=weapons.get(weapon); if(now-lastShot<we.cooldown||we.ammo<=0||gameOver)return; lastShot=now; we.ammo--; shake=4;
        Entity hit=null; float best=we.range; for(Entity e:enemies) if(e.alive){float dx=e.x-px,dy=e.y-py,d=(float)Math.hypot(dx,dy); float da=(float)Math.atan2(dy,dx)-angle; while(da>Math.PI)da-=2*Math.PI; while(da<-Math.PI)da+=2*Math.PI; if(d<best&&Math.abs(da)<0.18){best=d;hit=e;}}
        if(hit!=null){hit.hp-=(int)we.damage;if(hit.hp<=0){hit.alive=false;score+=100;}}
        else { for(Entity e:enemies) if(e.alive&&we.explosive){float d=(float)Math.hypot(e.x-px,e.y-py); if(d<260){e.hp-=70;if(e.hp<=0){e.alive=false;score+=100;}} } }
    }
    @Override public boolean onTouchEvent(MotionEvent ev){ float x=ev.getX(), y=ev.getY(), w=getWidth(),h=getHeight();
        switch(ev.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if(x<w*0.45f && y>h*0.55f){joyActive=true; float dx=x-110,dy=y-(h-115); float len=(float)Math.hypot(dx,dy); if(len>65){dx=dx*65/len;dy=dy*65/len;} joyX=dx/65f; joyY=dy/65f; px+=joyX*8;py+=joyY*8; clamp(); tx=w/2f-px*mapScale;ty=h/2f-py*mapScale;}
                else if(x>w*0.78f && y>h*0.65f) shoot();
                else if(y>h-110 && x>w-410){int col=(int)((x-(w-390))/48); int row=(int)((h-72-y)/46); int idx=row*8+col;if(idx>=0&&idx<weapons.size())weapon=idx;}
                break;
            case MotionEvent.ACTION_UP: joyActive=false;joyX=joyY=0;break;
        } return true;
    }
    void clamp(){px=Math.max(80,Math.min(2500,px));py=Math.max(80,Math.min(1800,py));}
}
