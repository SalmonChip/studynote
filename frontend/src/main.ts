import { createApp } from 'vue'
import { createPinia } from 'pinia'
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Checkbox,
  ConfigProvider,
  DatePicker,
  Divider,
  Drawer,
  Dropdown,
  Empty,
  Input,
  List,
  Menu,
  Modal,
  Pagination,
  Popconfirm,
  Result,
  Select,
  Segmented,
  Skeleton,
  Space,
  Switch,
  Table,
  Tag,
  Tree,
  message,
} from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import { router } from './router'
import { useSession } from './stores/session'
import { AUTH_EXPIRED } from './request/fetchClient'
import './style.css'

const app = createApp(App)
app.use(createPinia())
const session = useSession()
window.addEventListener(AUTH_EXPIRED, () => {
  session.logout()
  session.loginOpen = true
})
app.config.errorHandler = (error) => {
  console.error(error)
  message.error('页面发生错误，请刷新后重试')
}
for (const component of [
  Alert,
  Avatar,
  Badge,
  Button,
  Checkbox,
  ConfigProvider,
  DatePicker,
  Divider,
  Drawer,
  Dropdown,
  Empty,
  Input,
  List,
  Menu,
  Modal,
  Pagination,
  Popconfirm,
  Result,
  Select,
  Segmented,
  Skeleton,
  Space,
  Switch,
  Table,
  Tag,
  Tree,
])
  app.use(component)
app.use(router).mount('#app')
