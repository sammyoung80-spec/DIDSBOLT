import { createClient, SupabaseClient } from '@supabase/supabase-js';

// --- ENTITIES & INTERFACES ---
export interface UserEntity {
  id: string;
  username: string;
  accessExpiresAt: number | null; // Milliseconds timestamp
  isActive: boolean;
}

export interface VoucherEntity {
  code: string;
  days: number;
  isRedeemed: boolean;
  redeemedBy: string | null;
}

export interface AssetEntity {
  name: string;
  isOtc: boolean;
}

export interface PricePlanEntity {
  id: number;
  name: string;
  price: string;
  days: number;
  desc: string;
}

export interface SystemConfigEntity {
  key: string;
  value: string;
}

export interface SignalHistoryEntity {
  id: number;
  pair: string;
  direction: 'CALL' | 'PUT';
  result: 'WIN' | 'LOSS';
  time: string;
  timestamp: number;
}

// Manage dynamic DB client
let supabase: SupabaseClient | null = null;

// Retrieve from environment variables or custom local overrides
export const getSupabaseConfig = () => {
  const envUrl = import.meta.env.VITE_SUPABASE_URL || '';
  const envKey = import.meta.env.VITE_SUPABASE_ANON_KEY || '';
  
  const savedUrl = localStorage.getItem('DIDS_OVERRIDE_SUPABASE_URL') || '';
  const savedKey = localStorage.getItem('DIDS_OVERRIDE_SUPABASE_KEY') || '';
  
  const finalUrl = savedUrl || envUrl;
  const finalKey = savedKey || envKey;
  
  const isEnabled = finalUrl.trim().startsWith('http') && finalKey.trim().length > 10;
  
  return {
    url: finalUrl,
    key: finalKey,
    isEnabled
  };
};

// Initialize Supabase client
export const initSupabaseClient = () => {
  const config = getSupabaseConfig();
  if (config.isEnabled) {
    try {
      supabase = createClient(config.url, config.key, {
        auth: {
          persistSession: true
        }
      });
      console.log('Supabase initialized successfully to url:', config.url);
    } catch (e) {
      console.error('Supabase client failed to initialize:', e);
      supabase = null;
    }
  } else {
    supabase = null;
    console.log('Using browser LocalStorage database instead of Supabase.');
  }
};

// Seeding standard data in LocalStorage if blank
const SEED_DATA_KEY = 'didsbolt_seeded';
const SEED_USERS_KEY = 'didsbolt_users';
const SEED_VOUCHERS_KEY = 'didsbolt_vouchers';
const SEED_ASSETS_KEY = 'didsbolt_assets';
const SEED_PLANS_KEY = 'didsbolt_pricing_plans';
const SEED_CONFIG_KEY = 'didsbolt_system_config';
const SEED_HISTORY_KEY = 'didsbolt_signal_history';

export const seedInitialLocalStorageData = () => {
  if (localStorage.getItem(SEED_DATA_KEY) === 'true') return;
  
  const now = Date.now();
  
  // 1. Users
  const users: UserEntity[] = [
    { id: 'U1', username: '@trader_joe', accessExpiresAt: now + 86400000 * 2, isActive: true },
    { id: 'U2', username: '@binary_king', accessExpiresAt: null, isActive: true },
    { id: 'U3', username: '@dids_fan', accessExpiresAt: now - 3600000, isActive: false },
  ];
  localStorage.setItem(SEED_USERS_KEY, JSON.stringify(users));

  // 2. Vouchers
  const vouchers: VoucherEntity[] = [
    { code: 'DIDS-FREE-PASS', days: 1, isRedeemed: false, redeemedBy: null },
    { code: 'DIDS-PRO-WK99', days: 7, isRedeemed: false, redeemedBy: null },
  ];
  localStorage.setItem(SEED_VOUCHERS_KEY, JSON.stringify(vouchers));

  // 3. Assets
  const assets: AssetEntity[] = [
    // Standard Pairs
    { name: 'EUR/USD', isOtc: false },
    { name: 'GBP/USD', isOtc: false },
    { name: 'AUD/USD', isOtc: false },
    { name: 'USD/JPY', isOtc: false },
    { name: 'USD/CAD', isOtc: false },
    { name: 'GBP/JPY', isOtc: false },
    { name: 'BTC/USD', isOtc: false },
    { name: 'ETH/USD', isOtc: false },
    { name: 'SOL/USD', isOtc: false },
    { name: 'XAU/USD', isOtc: false },
    // OTC Pairs
    { name: 'EUR/USD OTC', isOtc: true },
    { name: 'GBP/USD OTC', isOtc: true },
    { name: 'AUD/CAD OTC', isOtc: true },
    { name: 'USD/JPY OTC', isOtc: true },
    { name: 'GBP/JPY OTC', isOtc: true },
    { name: 'NZD/USD OTC', isOtc: true },
    { name: 'EUR/JPY OTC', isOtc: true },
    { name: 'EUR/GBP OTC', isOtc: true },
    { name: 'USD/CHF OTC', isOtc: true },
    { name: 'CAD/JPY OTC', isOtc: true },
    { name: 'AUD/USD OTC', isOtc: true },
    { name: 'NZD/CAD OTC', isOtc: true },
    { name: 'GBP/CHF OTC', isOtc: true },
    { name: 'USD/INR OTC', isOtc: true }
  ];
  localStorage.setItem(SEED_ASSETS_KEY, JSON.stringify(assets));

  // 4. Pricing Plans
  const plans: PricePlanEntity[] = [
    { id: 1, name: '24 Hour Pass', price: '$5.00', days: 1, desc: 'Test the algorithm accuracy' },
    { id: 2, name: '3 Days Pro Pass', price: '$10.00', days: 3, desc: 'Most selected package' },
    { id: 3, name: 'Weekly Access Pass', price: '$20.00', days: 7, desc: 'Best value configuration' }
  ];
  localStorage.setItem(SEED_PLANS_KEY, JSON.stringify(plans));

  // 5. System Configurations
  const config = {
    'announcement': '🚨 LIVE DIDSBOLT TERMINAL SECURE — REDEEM ACCESS CODE TO BEGIN GENERATING AUTOMATED SIGNALS',
    'pocket_option_link': 'https://pocketoption.com/register/'
  };
  localStorage.setItem(SEED_CONFIG_KEY, JSON.stringify(config));

  // 6. Signal History
  const history: SignalHistoryEntity[] = [
    { id: 1, pair: 'EUR/USD OTC', direction: 'CALL', result: 'WIN', time: '10:45 AM', timestamp: now - 600000 },
    { id: 2, pair: 'AUD/CAD OTC', direction: 'PUT', result: 'LOSS', time: '09:30 AM', timestamp: now - 1800000 },
    { id: 3, pair: 'GBP/JPY OTC', direction: 'CALL', result: 'WIN', time: 'Yesterday', timestamp: now - 86400000 }
  ];
  localStorage.setItem(SEED_HISTORY_KEY, JSON.stringify(history));

  localStorage.setItem(SEED_DATA_KEY, 'true');
  console.log('Simulated database seeded successfully.');
};

// --- DATABASE FUNCTIONS (HYBRID REFRESH WITH AUTO BACKUP) ---

export const getLocalStorageItem = <T>(key: string, fallback: T): T => {
  const data = localStorage.getItem(key);
  return data ? JSON.parse(data) : fallback;
};

export const setLocalStorageItem = <T>(key: string, value: T) => {
  localStorage.setItem(key, JSON.stringify(value));
};

// USERS OPERATIONS
export const fetchUsers = async (): Promise<UserEntity[]> => {
  if (supabase) {
    try {
      const { data, error } = await supabase.from('users').select('*').order('username');
      if (!error && data) return data as UserEntity[];
    } catch (e) {
      console.error('Supabase query failed', e);
    }
  }
  return getLocalStorageItem<UserEntity[]>(SEED_USERS_KEY, []);
};

export const insertUser = async (user: UserEntity): Promise<void> => {
  const localList = getLocalStorageItem<UserEntity[]>(SEED_USERS_KEY, []);
  const existingIndex = localList.findIndex((u) => u.id === user.id || u.username === user.username);
  if (existingIndex >= 0) {
    localList[existingIndex] = user;
  } else {
    localList.push(user);
  }
  setLocalStorageItem(SEED_USERS_KEY, localList);

  if (supabase) {
    try {
      await supabase.from('users').upsert(user);
    } catch (e) {
      console.error('Supabase write error', e);
    }
  }
};

export const deleteUserById = async (id: string): Promise<void> => {
  const localList = getLocalStorageItem<UserEntity[]>(SEED_USERS_KEY, []);
  const updated = localList.filter((u) => u.id !== id);
  setLocalStorageItem(SEED_USERS_KEY, updated);

  if (supabase) {
    try {
      await supabase.from('users').delete().eq('id', id);
    } catch (e) {
      console.error('Supabase delete error', e);
    }
  }
};

// VOUCHERS OPERATIONS
export const fetchVouchers = async (): Promise<VoucherEntity[]> => {
  if (supabase) {
    try {
      const { data, error } = await supabase.from('vouchers').select('*').order('code');
      if (!error && data) return data as VoucherEntity[];
    } catch (e) {
      console.error('Supabase query failed', e);
    }
  }
  return getLocalStorageItem<VoucherEntity[]>(SEED_VOUCHERS_KEY, []);
};

export const insertVoucher = async (voucher: VoucherEntity): Promise<void> => {
  const localList = getLocalStorageItem<VoucherEntity[]>(SEED_VOUCHERS_KEY, []);
  const existingIdx = localList.findIndex((v) => v.code === voucher.code);
  if (existingIdx >= 0) {
    localList[existingIdx] = voucher;
  } else {
    localList.push(voucher);
  }
  setLocalStorageItem(SEED_VOUCHERS_KEY, localList);

  if (supabase) {
    try {
      await supabase.from('vouchers').upsert(voucher);
    } catch (e) {
      console.error('Supabase write error', e);
    }
  }
};

export const deleteVoucherByCode = async (code: string): Promise<void> => {
  const localList = getLocalStorageItem<VoucherEntity[]>(SEED_VOUCHERS_KEY, []);
  const updated = localList.filter((v) => v.code !== code);
  setLocalStorageItem(SEED_VOUCHERS_KEY, updated);

  if (supabase) {
    try {
      await supabase.from('vouchers').delete().eq('code', code);
    } catch (e) {
      console.error('Supabase delete error', e);
    }
  }
};

// ASSETS OPERATIONS
export const fetchAssets = async (): Promise<AssetEntity[]> => {
  const localList = getLocalStorageItem<AssetEntity[]>(SEED_ASSETS_KEY, []);
  // Return list with standard sorted order
  return localList.sort((a, b) => a.name.localeCompare(b.name));
};

export const insertAsset = async (asset: AssetEntity): Promise<void> => {
  const localList = getLocalStorageItem<AssetEntity[]>(SEED_ASSETS_KEY, []);
  if (!localList.some((a) => a.name === asset.name)) {
    localList.push(asset);
    setLocalStorageItem(SEED_ASSETS_KEY, localList);
  }
};

export const deleteAssetByName = async (name: string): Promise<void> => {
  const localList = getLocalStorageItem<AssetEntity[]>(SEED_ASSETS_KEY, []);
  const updated = localList.filter((a) => a.name !== name);
  setLocalStorageItem(SEED_ASSETS_KEY, updated);
};

// PRICING PLANS OPERATIONS
export const fetchPricePlans = async (): Promise<PricePlanEntity[]> => {
  return getLocalStorageItem<PricePlanEntity[]>(SEED_PLANS_KEY, []);
};

export const insertPricePlan = async (plan: PricePlanEntity): Promise<void> => {
  const localList = getLocalStorageItem<PricePlanEntity[]>(SEED_PLANS_KEY, []);
  const existingIndex = localList.findIndex((p) => p.id === plan.id);
  if (existingIndex >= 0) {
    localList[existingIndex] = plan;
  } else {
    localList.push(plan);
  }
  setLocalStorageItem(SEED_PLANS_KEY, localList);
};

export const deletePricePlanById = async (id: number): Promise<void> => {
  const localList = getLocalStorageItem<PricePlanEntity[]>(SEED_PLANS_KEY, []);
  const updated = localList.filter((p) => p.id !== id);
  setLocalStorageItem(SEED_PLANS_KEY, updated);
};

// SYSTEM CONFIG OPERATIONS
export const fetchSystemConfig = async (key: string, defaultValue: string): Promise<string> => {
  if (supabase) {
    try {
      const { data, error } = await supabase.from('system_config').select('value').eq('key', key).single();
      if (!error && data) return data.value;
    } catch (e) {
      console.error('Supabase config fetch failed', e);
    }
  }
  const config = getLocalStorageItem<Record<string, string>>(SEED_CONFIG_KEY, {});
  return config[key] || defaultValue;
};

export const insertSystemConfig = async (key: string, value: string): Promise<void> => {
  const config = getLocalStorageItem<Record<string, string>>(SEED_CONFIG_KEY, {});
  config[key] = value;
  setLocalStorageItem(SEED_CONFIG_KEY, config);

  if (supabase) {
    try {
      await supabase.from('system_config').upsert({ key, value });
    } catch (e) {
      console.error('Supabase write config failed', e);
    }
  }
};

// SIGNAL HISTORY OPERATIONS
export const fetchSignalHistory = async (): Promise<SignalHistoryEntity[]> => {
  if (supabase) {
    try {
      const { data, error } = await supabase.from('signal_history').select('*').order('timestamp', { ascending: false }).limit(50);
      if (!error && data) return data as SignalHistoryEntity[];
    } catch (e) {
      console.error('Supabase history retrieve failed', e);
    }
  }
  return getLocalStorageItem<SignalHistoryEntity[]>(SEED_HISTORY_KEY, []);
};

export const insertSignal = async (signal: Omit<SignalHistoryEntity, 'id'>): Promise<void> => {
  const localList = getLocalStorageItem<SignalHistoryEntity[]>(SEED_HISTORY_KEY, []);
  const nextId = localList.length ? Math.max(...localList.map((h) => h.id)) + 1 : 1;
  const newSignal: SignalHistoryEntity = { ...signal, id: nextId };
  localList.unshift(newSignal);
  setLocalStorageItem(SEED_HISTORY_KEY, localList);

  if (supabase) {
    try {
      await supabase.from('signal_history').insert(newSignal);
    } catch (e) {
      console.error('Supabase signal insert failed', e);
    }
  }
};

export const clearSignalHistory = async (): Promise<void> => {
  setLocalStorageItem(SEED_HISTORY_KEY, []);
  if (supabase) {
    try {
      await supabase.from('signal_history').delete().gt('id', 0);
    } catch (e) {
      console.error('Supabase clear history failed', e);
    }
  }
};
