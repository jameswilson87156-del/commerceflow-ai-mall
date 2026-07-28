# P5C Layout And Scroll Evidence

At 1920x1080, all three work areas share a stable 948px grid height:

- SKU selector: 418.41px wide, independently scrollable.
- Chat workbench: 674.09px wide, with a fixed composer and an internal message list.
- Facts/Evidence/Trace: 483.50px wide, with independent section scrolling.

Five real visible customer-service requests were sent through the Vue page. The observed message counts were 2, 4, 6, 8, and 10. After the fifth response:

| Measurement | Result |
| --- | ---: |
| Document client height / scroll height | 1080 / 1080 |
| Window scroll top | 0 |
| Chat list client height / scroll height | 612 / 938 |
| Chat list scroll top after latest answer | 326 |
| Chat list overflow | `auto` |
| SKU list client height / scroll height | 737 / 737 |
| SKU list overflow | `auto` |

This proves the accumulated conversation stays in the message container and the desktop page does not grow beyond the viewport. The scroll implementation follows new messages without moving the window or taking composer focus; a reader who has manually moved away from the bottom is not repeatedly pulled back by asynchronous response updates.
