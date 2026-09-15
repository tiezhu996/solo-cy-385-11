import { createApp } from 'vue';
import { Button, Cell, CellGroup, Field, Tag } from 'vant';
import 'vant/lib/index.css';
import App from './App.vue';
import './styles.css';

createApp(App).use(Button).use(Cell).use(CellGroup).use(Field).use(Tag).mount('#app');
