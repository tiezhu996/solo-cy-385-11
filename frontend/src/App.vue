<template>
  <main>
    <header><h1>宝宝成长记录</h1><p>小满 10 个月 · 今日辅食 3 次</p></header>

    <section class="card">
      <h2>家庭协作</h2>
      <div v-if="!user">
        <van-field v-model="nickname" placeholder="昵称" />
        <van-field v-model="password" type="password" placeholder="密码（至少 6 位）" />
        <div class="row">
          <van-button size="small" type="primary" @click="onRegister">注册并登录</van-button>
          <van-button size="small" @click="onLogin">登录</van-button>
          <van-button v-if="needsSetPassword" size="small" type="warning" @click="onSetPassword">
            设置初始密码
          </van-button>
        </div>
        <p v-if="needsSetPassword" class="hint">该账号为旧账号，尚未设置密码，设置后即可登录。</p>
      </div>
      <template v-else>
        <div class="row">
          <span>当前用户：{{ user.nickname }}</span>
          <van-button size="mini" @click="onLogout">退出登录</van-button>
        </div>

        <h3>我的宝宝</h3>
        <div class="row">
          <select v-model.number="currentBabyId" @change="refreshFamily">
            <option v-for="b in babies" :key="b.id" :value="b.id">
              {{ b.name }}（{{ roleText(b.role) }}）
            </option>
          </select>
          <van-button size="mini" @click="loadBabies">刷新</van-button>
        </div>
        <div class="row">
          <van-field v-model="newBabyName" placeholder="宝宝姓名" />
          <van-field v-model="newBabyBirthday" placeholder="出生日期 如 2025-11-01" />
          <van-button size="small" type="primary" @click="onCreateBaby">创建档案</van-button>
        </div>

        <template v-if="adoptable.length">
          <h3>待认领的旧档案</h3>
          <van-cell
            v-for="b in adoptable"
            :key="b.id"
            :title="b.name"
            :label="`出生于 ${b.birthday} · 升级前创建，暂无创建者`"
          >
            <template #value>
              <van-button size="mini" type="primary" @click="onAdopt(b)">认领为我的宝宝</van-button>
            </template>
          </van-cell>
        </template>

        <template v-if="currentBaby">
          <h3>成员与权限</h3>
          <van-cell
            v-for="m in members"
            :key="m.id"
            :title="m.nickname || `用户${m.userId}`"
          >
            <template #value><van-tag>{{ roleText(m.role) }}</van-tag></template>
            <template v-if="canManage && m.role !== 'OWNER'" #label>
              <select v-model="roleDraft[m.userId]">
                <option value="VIEW">查看</option>
                <option value="RECORD">记录</option>
                <option value="MANAGE">管理</option>
              </select>
              <van-button size="mini" @click="onChangeRole(m)">保存角色</van-button>
              <van-button size="mini" type="danger" @click="onRemoveMember(m)">移除</van-button>
            </template>
          </van-cell>

          <template v-if="canManage">
            <h3>一次性邀请码</h3>
            <div class="row">
              <select v-model="inviteRole">
                <option value="VIEW">查看</option>
                <option value="RECORD">记录</option>
                <option value="MANAGE">管理</option>
              </select>
              <van-button size="small" type="primary" @click="onCreateInvite">生成邀请码</van-button>
            </div>
            <van-cell
              v-for="inv in invites"
              :key="inv.id"
              :title="inv.code"
              :label="`${roleText(inv.role)} · ${statusText(inv.status)} · 截止 ${inv.expiresAt}`"
            >
              <template v-if="inv.status === 'ACTIVE'" #value>
                <van-button size="mini" type="warning" @click="onRevokeInvite(inv)">撤销</van-button>
              </template>
            </van-cell>
          </template>

          <van-button
            v-if="currentBaby.role !== 'OWNER'"
            size="small"
            type="danger"
            @click="onLeave"
          >退出家庭</van-button>
        </template>

        <h3>领取邀请</h3>
        <div class="row">
          <van-field v-model="claimCode" placeholder="输入邀请码" />
          <van-button size="small" type="primary" @click="onClaim">领取</van-button>
        </div>
      </template>
      <p v-if="familyError" class="error">{{ familyError }}</p>
    </section>

    <section class="card">
      <h2>生长曲线</h2>
      <div ref="growthChart" class="chart"></div>
    </section>
    <section class="card">
      <h2>疫苗提醒</h2>
      <van-cell v-for="item in vaccines" :key="item.name" :title="item.name" :value="item.date"><template #label><van-tag :type="item.done ? 'success' : 'warning'">{{ item.done ? '已接种' : '待接种' }}</van-tag></template></van-cell>
    </section>
    <section class="card">
      <h2>辅食推荐</h2>
      <van-cell v-for="food in foods" :key="food" :title="food" value="适合 9-12 个月" />
    </section>
  </main>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import * as echarts from 'echarts';
import { api, ApiError, clearToken, setToken } from './api';
import type { Baby, BabyView, Invite, Member, User } from './api';

const growthChart = ref<HTMLElement>();
const vaccines = [{ name: '麻腮风疫苗', date: '2026-06-18', done: false }, { name: '乙肝疫苗', date: '2026-04-10', done: true }];
const foods = ['南瓜米糊', '鳕鱼土豆泥', '苹果燕麦粥'];

// ---------- 家庭协作 ----------
const user = ref<User | null>(null);
const nickname = ref('');
const password = ref('');
const needsSetPassword = ref(false);
const babies = ref<BabyView[]>([]);
const adoptable = ref<Baby[]>([]);
const currentBabyId = ref<number>(0);
const newBabyName = ref('');
const newBabyBirthday = ref('');
const members = ref<Member[]>([]);
const invites = ref<Invite[]>([]);
const inviteRole = ref('VIEW');
const claimCode = ref('');
const familyError = ref('');
const roleDraft = reactive<Record<number, string>>({});

const currentBaby = computed(() => babies.value.find(b => b.id === currentBabyId.value) ?? null);
const canManage = computed(() => currentBaby.value !== null && ['OWNER', 'MANAGE'].includes(currentBaby.value.role));

function roleText(role: string) {
  return { OWNER: '创建者', MANAGE: '管理', RECORD: '记录', VIEW: '查看' }[role] ?? role;
}
function statusText(status: string) {
  return { ACTIVE: '待领取', CLAIMED: '已领取', REVOKED: '已撤销' }[status] ?? status;
}

async function run(action: () => Promise<void>) {
  familyError.value = '';
  try {
    await action();
  } catch (e) {
    if (e instanceof ApiError && e.code === 'PASSWORD_NOT_SET') needsSetPassword.value = true;
    familyError.value = e instanceof Error ? e.message : '操作失败';
  }
}

function saveAuth(payload: { token: string; user: User }) {
  setToken(payload.token);
  user.value = payload.user;
  needsSetPassword.value = false;
  password.value = '';
}

async function onRegister() {
  await run(async () => {
    saveAuth(await api.register(nickname.value.trim(), password.value));
    await loadBabies();
  });
}

async function onLogin() {
  await run(async () => {
    saveAuth(await api.login(nickname.value.trim(), password.value));
    await loadBabies();
  });
}

async function onSetPassword() {
  await run(async () => {
    saveAuth(await api.setInitialPassword(nickname.value.trim(), password.value));
    await loadBabies();
  });
}

function onLogout() {
  clearToken();
  user.value = null;
  babies.value = [];
  adoptable.value = [];
  members.value = [];
  invites.value = [];
  currentBabyId.value = 0;
}

async function loadBabies() {
  await run(async () => {
    babies.value = await api.myBabies();
    adoptable.value = await api.adoptableBabies();
    if (!babies.value.some(b => b.id === currentBabyId.value)) {
      currentBabyId.value = babies.value[0]?.id ?? 0;
    }
    await refreshFamily();
  });
}

async function onCreateBaby() {
  await run(async () => {
    await api.createBaby({ name: newBabyName.value.trim(), birthday: newBabyBirthday.value.trim() });
    newBabyName.value = '';
    newBabyBirthday.value = '';
  });
  await loadBabies();
}

async function onAdopt(b: Baby) {
  await run(async () => { await api.adoptBaby(b.id); });
  await loadBabies();
}

async function refreshFamily() {
  members.value = [];
  invites.value = [];
  if (!currentBaby.value) return;
  await run(async () => {
    members.value = await api.members(currentBabyId.value);
    members.value.forEach(m => { roleDraft[m.userId] = m.role; });
    if (canManage.value) invites.value = await api.invites(currentBabyId.value);
  });
}

async function onChangeRole(m: Member) {
  await run(async () => { await api.changeRole(currentBabyId.value, m.userId, roleDraft[m.userId]); });
  await refreshFamily();
}

async function onRemoveMember(m: Member) {
  await run(async () => { await api.removeMember(currentBabyId.value, m.userId); });
  await refreshFamily();
}

async function onLeave() {
  await run(async () => { await api.leave(currentBabyId.value); });
  await loadBabies();
}

async function onCreateInvite() {
  await run(async () => { await api.createInvite(currentBabyId.value, inviteRole.value); });
  await refreshFamily();
}

async function onRevokeInvite(inv: Invite) {
  await run(async () => { await api.revokeInvite(currentBabyId.value, inv.id); });
  await refreshFamily();
}

async function onClaim() {
  await run(async () => {
    await api.claimInvite(claimCode.value.trim());
    claimCode.value = '';
  });
  await loadBabies();
}

onMounted(() => {
  const chart = echarts.init(growthChart.value!);
  chart.setOption({ legend: {}, xAxis: { data: ['6月','7月','8月','9月','10月'] }, yAxis: {}, series: [{ name: '体重kg', type: 'line', data: [7.5,7.9,8.2,8.6,9.1] }, { name: '身高cm', type: 'line', data: [66,68,70,72,74] }] });
});
</script>
