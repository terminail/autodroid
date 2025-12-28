from typing import Optional
import uiautomator2 as u2


class U2Device:
    def __init__(self, device_id: str = None):
        self._device_id: Optional[str] = device_id
        self._d: Optional[u2.Device] = None

    @property
    def device_id(self) -> Optional[str]:
        return self._device_id

    @property
    def d(self) -> u2.Device:
        if self._d is None:
            self._d = u2.connect(self._device_id)
        return self._d

    @property
    def info(self) -> dict:
        return self.d.info

    @property
    def xml(self) -> str:
        return self.d.dump_hierarchy()

    def dump_hierarchy(self) -> str:
        return self.d.dump_hierarchy()

    def click(self, x: int, y: int) -> bool:
        try:
            self.d.click(x, y)
            return True
        except Exception:
            return False

    def long_click(self, x: int, y: int, duration: float = 0.5) -> bool:
        try:
            self.d.long_click(x, y, duration)
            return True
        except Exception:
            return False

    def swipe(self, x1: int, y1: int, x2: int, y2: int, duration: float = 0.5) -> bool:
        try:
            self.d.swipe(x1, y1, x2, y2, duration)
            return True
        except Exception:
            return False

    def send_keys(self, text: str) -> bool:
        try:
            self.d.send_keys(text)
            return True
        except Exception:
            return False

    def clear_text(self) -> bool:
        try:
            self.d.clear_text()
            return True
        except Exception:
            return False

    def press(self, key: int) -> bool:
        try:
            self.d.press(key)
            return True
        except Exception:
            return False

    def press_back(self) -> bool:
        return self.press(4)

    def press_home(self) -> bool:
        return self.press(3)

    def press_menu(self) -> bool:
        return self.press(82)

    def exists(self, **kwargs) -> bool:
        return self.d.exists(**kwargs)

    def __getattr__(self, name):
        return getattr(self.d, name)
