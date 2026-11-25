import { useRef } from 'react';

interface UseSwipeGestureOptions {
  onSwipeLeft?: () => void;
  onSwipeRight?: () => void;
  threshold?: number;
}

function isInsideHorizontalScroll(el: HTMLElement | null): boolean {
  let current: HTMLElement | null = el;
  while (current && current !== document.body && current !== document.documentElement) {
    if (current.hasAttribute('data-no-swipe')) {
      return true;
    }
    const style = window.getComputedStyle(current);
    const ox = style.overflowX;
    const isScrollableClass =
      current.classList.contains('overflow-x-auto') ||
      current.classList.contains('overflow-x-scroll');
    if (
      (isScrollableClass || ox === 'auto' || ox === 'scroll') &&
      current.scrollWidth > current.clientWidth + 1
    ) {
      return true;
    }
    current = current.parentElement;
  }
  return false;
}

export function useSwipeGesture({
  onSwipeLeft,
  onSwipeRight,
  threshold = 50,
}: UseSwipeGestureOptions) {
  const touchStartXRef = useRef(0);
  const touchStartYRef = useRef(0);
  const isTouchIgnoredRef = useRef(false);

  const mouseStartXRef = useRef(0);
  const mouseStartYRef = useRef(0);
  const isMouseDownRef = useRef(false);

  const handleTouchStart = (e: React.TouchEvent) => {
    const target = e.target as HTMLElement;
    if (isInsideHorizontalScroll(target)) {
      isTouchIgnoredRef.current = true;
      return;
    }
    isTouchIgnoredRef.current = false;
    touchStartXRef.current = e.touches[0].clientX;
    touchStartYRef.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (isTouchIgnoredRef.current) return;
    const deltaX = e.changedTouches[0].clientX - touchStartXRef.current;
    const deltaY = e.changedTouches[0].clientY - touchStartYRef.current;
    if (Math.abs(deltaX) > threshold && Math.abs(deltaX) > Math.abs(deltaY) * 1.2) {
      if (deltaX < 0) {
        onSwipeLeft?.();
      } else {
        onSwipeRight?.();
      }
    }
  };

  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.button !== 0) return;
    const target = e.target as HTMLElement;
    if (
      ['BUTTON', 'INPUT', 'TEXTAREA', 'SELECT', 'A'].includes(target.tagName) ||
      target.closest('button, a, input, textarea, label') ||
      isInsideHorizontalScroll(target)
    ) {
      return;
    }
    isMouseDownRef.current = true;
    mouseStartXRef.current = e.clientX;
    mouseStartYRef.current = e.clientY;
  };

  const handleMouseUp = (e: React.MouseEvent) => {
    if (!isMouseDownRef.current) return;
    isMouseDownRef.current = false;
    const deltaX = e.clientX - mouseStartXRef.current;
    const deltaY = e.clientY - mouseStartYRef.current;
    if (Math.abs(deltaX) > threshold && Math.abs(deltaX) > Math.abs(deltaY) * 1.2) {
      if (deltaX < 0) {
        onSwipeLeft?.();
      } else {
        onSwipeRight?.();
      }
    }
  };

  const handleMouseLeave = () => {
    isMouseDownRef.current = false;
  };

  return {
    onTouchStart: handleTouchStart,
    onTouchEnd: handleTouchEnd,
    onMouseDown: handleMouseDown,
    onMouseUp: handleMouseUp,
    onMouseLeave: handleMouseLeave,
  };
}
