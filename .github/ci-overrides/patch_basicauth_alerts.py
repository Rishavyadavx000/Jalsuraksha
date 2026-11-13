#!/usr/bin/env python3
"""Make Basic Auth failures first-class authentication failures in domain state."""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_basicauth_alerts.py <Android-project-root>")

root = Path(sys.argv[1]).resolve()

def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        print(f"Already patched: {label}")
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one target for {label}; found {count} in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"Patched: {label}")

main = root / "app/src/main/java/com/jalsuraksha/app/domain/AlertEngine.kt"
replace_once(
    main,
    '''            is ApiError.AuthRequired -> {
                if (!st.authFlagged) {
                    events += NewEvent(
                        Severity.WARNING,
                        EventType.AUTH_FAILED,
                        "The firmware rejected the control token (HTTP 401). Check the token saved in Settings > Device.",
                    )
                    st = st.copy(authFlagged = true)
                }
            }
''',
    '''            is ApiError.AuthRequired, is ApiError.BasicAuthRequired -> {
                if (!st.authFlagged) {
                    val authMessage = if (error is ApiError.BasicAuthRequired) {
                        "The ESP32 rejected HTTP Basic Auth (HTTP 401). Check WEB_USER and WEB_PASSWORD in Settings > Device."
                    } else {
                        "The firmware rejected the control token (HTTP 401). Check the token saved in Settings > Device."
                    }
                    events += NewEvent(
                        Severity.WARNING,
                        EventType.AUTH_FAILED,
                        authMessage,
                    )
                    st = st.copy(authFlagged = true)
                }
            }
''',
    "AlertEngine Basic Auth error branch",
)

main = root / "app/src/main/java/com/jalsuraksha/app/domain/LinkDeriver.kt"
replace_once(
    main,
    '            is ApiError.AuthRequired -> return LinkState.AUTH_REQUIRED\n',
    '            is ApiError.AuthRequired, is ApiError.BasicAuthRequired -> return LinkState.AUTH_REQUIRED\n',
    "LinkDeriver Basic Auth state",
)

test = root / "app/src/test/java/com/jalsuraksha/app/domain/AlertEngineTest.kt"
replace_once(
    test,
    '''        assertEquals(1, events.count { it.type == EventType.AUTH_FAILED })
    }
}''',
    '''        assertEquals(1, events.count { it.type == EventType.AUTH_FAILED })

        val basicAuth = AlertEngine.onFailure(
            AlertState(), ApiError.BasicAuthRequired, 1, null, 3_000L, t,
        )
        val basicAuthEvent = basicAuth.events.single { it.type == EventType.AUTH_FAILED }
        assertTrue(basicAuthEvent.message.contains("WEB_USER"))
    }
}''',
    "AlertEngine Basic Auth regression assertion",
)

test = root / "app/src/test/java/com/jalsuraksha/app/domain/LinkDeriverTest.kt"
replace_once(
    test,
    '''        assertEquals(LinkState.AUTH_REQUIRED, LinkDeriver.derive(true, snap(100_000L, 1, ApiError.AuthRequired), 101_000L, stale))
    }''',
    '''        assertEquals(LinkState.AUTH_REQUIRED, LinkDeriver.derive(true, snap(100_000L, 1, ApiError.AuthRequired), 101_000L, stale))
        assertEquals(LinkState.AUTH_REQUIRED, LinkDeriver.derive(true, snap(100_000L, 1, ApiError.BasicAuthRequired), 101_000L, stale))
    }''',
    "LinkDeriver Basic Auth regression assertion",
)

print("Basic Auth alert/link-state patches applied. Test annotation count is unchanged.")
