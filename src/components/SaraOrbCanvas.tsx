import { useEffect, useRef } from 'react';

export type OrbVisualState = 'IDLE' | 'ACTIVE_GREETING' | 'LISTENING_READY';

interface SaraOrbCanvasProps {
  state: OrbVisualState;
  onTap: () => void;
  size?: number;
  primaryRgb?: string;
  secondaryRgb?: string;
  animationsEnabled?: boolean;
  canvasBg?: string;
}

export function SaraOrbCanvas({
  state,
  onTap,
  size = 200,
  primaryRgb = '34, 211, 238',
  secondaryRgb = '139, 92, 246',
  animationsEnabled = true,
  canvasBg = '#060B18',
}: SaraOrbCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const stateRef = useRef<OrbVisualState>(state);
  const configRef = useRef({ primaryRgb, secondaryRgb, animationsEnabled, canvasBg });

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  useEffect(() => {
    configRef.current = { primaryRgb, secondaryRgb, animationsEnabled, canvasBg };
  }, [primaryRgb, secondaryRgb, animationsEnabled, canvasBg]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = size * dpr;
    canvas.height = size * dpr;
    ctx.scale(dpr, dpr);

    let animId = 0;
    let currentScale = 1;
    const start = performance.now();

    const prefersReduced =
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const draw = (now: number) => {
      const elapsed = (now - start) / 1000;
      const activeState = stateRef.current;
      const cfg = configRef.current;
      const shouldAnimate = cfg.animationsEnabled && !prefersReduced;

      const targetScale =
        activeState === 'IDLE' ? 1.0 : activeState === 'ACTIVE_GREETING' ? 1.08 : 1.13;
      currentScale += (targetScale - currentScale) * 0.1;

      const breathe = shouldAnimate ? 0.94 + 0.06 * Math.sin(elapsed * 2.2) : 1.0;
      const outerAngle = shouldAnimate ? (elapsed * (Math.PI * 2)) / 14 : 0.4;
      const innerAngle = shouldAnimate ? -(elapsed * (Math.PI * 2)) / 9 : 2.2;

      ctx.clearRect(0, 0, size, size);
      const cx = size / 2;
      const cy = size / 2;
      const baseRadius = size * 0.26 * breathe * currentScale;

      // 1. Ambient outer aura
      const auraGrad = ctx.createRadialGradient(cx, cy, baseRadius * 0.3, cx, cy, size * 0.5);
      const auraAlpha = activeState === 'IDLE' ? 0.22 : 0.38;
      auraGrad.addColorStop(0, `rgba(${cfg.primaryRgb}, ${auraAlpha})`);
      auraGrad.addColorStop(0.55, `rgba(${cfg.secondaryRgb}, 0.12)`);
      auraGrad.addColorStop(1, `rgba(${cfg.primaryRgb}, 0)`);
      ctx.fillStyle = auraGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, size * 0.5, 0, Math.PI * 2);
      ctx.fill();

      // 2. Outer primary orbital arc
      const outerR = size * 0.41;
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(outerAngle);
      const outerStroke = ctx.createLinearGradient(-outerR, -outerR, outerR, outerR);
      outerStroke.addColorStop(0, `rgba(${cfg.primaryRgb}, 0.05)`);
      outerStroke.addColorStop(0.5, `rgba(${cfg.primaryRgb}, 0.85)`);
      outerStroke.addColorStop(1, `rgba(${cfg.primaryRgb}, 0.98)`);
      ctx.strokeStyle = outerStroke;
      ctx.lineWidth = 2.5;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.arc(0, 0, outerR, 0, Math.PI * 1.22);
      ctx.stroke();
      ctx.restore();

      // 3. Inner counter-rotating secondary arc
      const innerR = size * 0.34;
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(innerAngle);
      const innerStroke = ctx.createLinearGradient(-innerR, innerR, innerR, -innerR);
      innerStroke.addColorStop(0, `rgba(${cfg.secondaryRgb}, 0.08)`);
      innerStroke.addColorStop(0.6, `rgba(${cfg.secondaryRgb}, 0.88)`);
      innerStroke.addColorStop(1, `rgba(${cfg.secondaryRgb}, 0.98)`);
      ctx.strokeStyle = innerStroke;
      ctx.lineWidth = 2;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.arc(0, 0, innerR, 0.6, 0.6 + Math.PI * 1.08);
      ctx.stroke();
      ctx.restore();

      // 4. Precision core ring
      ctx.strokeStyle = `rgba(${cfg.primaryRgb}, 0.42)`;
      ctx.lineWidth = 1.2;
      ctx.beginPath();
      ctx.arc(cx, cy, baseRadius * 1.13, 0, Math.PI * 2);
      ctx.stroke();

      // 5. Luminous core sphere
      const coreGrad = ctx.createRadialGradient(
        cx - baseRadius * 0.24,
        cy - baseRadius * 0.24,
        baseRadius * 0.08,
        cx,
        cy,
        baseRadius * 1.15
      );
      coreGrad.addColorStop(0, 'rgba(255, 255, 255, 0.92)');
      coreGrad.addColorStop(0.45, `rgba(${cfg.primaryRgb}, 0.85)`);
      coreGrad.addColorStop(0.82, `rgba(${cfg.secondaryRgb}, 0.48)`);
      coreGrad.addColorStop(1, cfg.canvasBg);

      ctx.fillStyle = coreGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, baseRadius, 0, Math.PI * 2);
      ctx.fill();

      animId = requestAnimationFrame(draw);
    };

    animId = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(animId);
  }, [size]);

  return (
    <button
      type="button"
      onClick={onTap}
      aria-label="Interact with SARA Orb"
      className="relative flex items-center justify-center rounded-full focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-cyan-400 cursor-pointer transition-transform active:scale-95"
      style={{ width: size, height: size }}
    >
      <canvas ref={canvasRef} style={{ width: size, height: size }} />
    </button>
  );
}
