(() => {

  const CATEGORIES = [
    'FOOD',
    'TRAVEL',
    'SHOPPING',
    'HEALTH',
    'EDUCATION',
    'ENTERTAINMENT',
    'BILLS',
    'SALARY',
    'INVESTMENT',
    'OTHER'
  ];

  const TYPES = ['INCOME', 'EXPENSE'];

  const $ = s => document.querySelector(s);
  const $$ = s => Array.from(document.querySelectorAll(s));

  const state = {
    token: localStorage.getItem('nexspend.accessToken'),
    refresh: localStorage.getItem('nexspend.refreshToken'),
    user: JSON.parse(localStorage.getItem('nexspend.user') || 'null'),
    page: 'dashboard',
    expensePage: 0
  };


  /* =========================================================
     API
     ========================================================= */

  const api = {

    request: async (path, options = {}, retried = false) => {

      const headers = {
        ...(options.body && !(options.body instanceof FormData)
          ? { 'Content-Type': 'application/json' }
          : {}),
        ...(options.headers || {})
      };

      if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
      }

      let response;

      try {

        response = await fetch(path, {
          ...options,
          headers
        });

      } catch {

        throw new Error(
          'Cannot reach the server. Start NexSpend on port 8080.'
        );

      }

      if (
        (response.status === 401 || response.status === 403) &&
        state.refresh &&
        !retried
      ) {

        await api.refresh();

        return api.request(
          path,
          options,
          true
        );

      }

      if (!response.ok) {

        const raw = await response.text();

        let data;

        try {
          data = JSON.parse(raw);
        } catch {
          data = null;
        }

        if (response.status === 401 || response.status === 403) {
          logout();

          throw new Error(
            'Your session has expired. Please sign in again.'
          );
        }

        throw new Error(
          data?.message ||
          data?.error ||
          raw ||
          `Request failed (${response.status})`
        );

      }

      if (response.status === 204) {
        return null;
      }

      const type =
        response.headers.get('content-type') || '';

      return type.includes('application/json')
        ? response.json()
        : response.text();
    },


    register: body =>
      api.request(
        '/api/auth/register',
        {
          method: 'POST',
          body: JSON.stringify(body)
        }
      ),


    login: body =>
      api.request(
        '/api/auth/login',
        {
          method: 'POST',
          body: JSON.stringify(body)
        }
      ),


    refresh: async () => {

      const r = await api.request(
        '/api/auth/refresh',
        {
          method: 'POST',
          body: JSON.stringify({
            refreshToken: state.refresh
          })
        },
        true
      );

      state.token = r.accessToken;

      localStorage.setItem(
        'nexspend.accessToken',
        r.accessToken
      );

    },


    dashboard: () =>
      api.request('/api/dashboard'),


    expenses: q =>
      api.request(
        `/api/expenses?${new URLSearchParams(q)}`
      ),


    expense: id =>
      api.request(`/api/expenses/${id}`),


    createExpense: b =>
      api.request(
        '/api/expenses',
        {
          method: 'POST',
          body: JSON.stringify(b)
        }
      ),


    updateExpense: (id, b) =>
      api.request(
        `/api/expenses/${id}`,
        {
          method: 'PUT',
          body: JSON.stringify(b)
        }
      ),


    deleteExpense: id =>
      api.request(
        `/api/expenses/${id}`,
        {
          method: 'DELETE'
        }
      ),


    filterExpenses: q =>
      api.request(
        `/api/expenses/filter?${new URLSearchParams(q)}&page=0&size=50`
      ),


    budgets: () =>
      api.request('/api/budgets'),


    currentBudgets: () =>
      api.request('/api/budgets/current-month'),


    saveBudget: b =>
      api.request(
        '/api/budgets',
        {
          method: 'POST',
          body: JSON.stringify(b)
        }
      ),


    recurring: () =>
      api.request('/api/recurring'),


    createRecurring: b =>
      api.request(
        '/api/recurring',
        {
          method: 'POST',
          body: JSON.stringify(b)
        }
      ),


    me: () =>
      api.request('/api/users/me'),


    updateUser: (id, b) =>
      api.request(
        `/api/users/${id}`,
        {
          method: 'PUT',
          body: JSON.stringify(b)
        }
      ),


    // ---------------------------------------------------------
    // CHANGE PASSWORD
    // Existing /api/users/... structure — no new auth API.
    // ---------------------------------------------------------
    changePassword: (id, b) =>
      api.request(
        `/api/users/${id}/change-password`,
        {
          method: 'PUT',
          body: JSON.stringify(b)
        }
      ),


    deleteUser: id =>
      api.request(
        `/api/users/${id}`,
        {
          method: 'DELETE'
        }
      ),


    notify: path =>
      api.request(`/api/notification/${path}`),


    download: async (path, name, retried = false) => {

      const r = await fetch(path, {
        headers: {
          Authorization: `Bearer ${state.token}`
        }
      });

      if (
        (r.status === 401 || r.status === 403) &&
        state.refresh &&
        !retried
      ) {

        await api.refresh();

        return api.download(
          path,
          name,
          true
        );

      }

      if (!r.ok) {

        const raw = await r.text();

        try {

          const error = JSON.parse(raw);

          throw new Error(
            error.message ||
            error.error ||
            raw
          );

        } catch (parseError) {

          if (parseError instanceof SyntaxError) {

            throw new Error(
              raw ||
              `Request failed (${r.status})`
            );

          }

          throw parseError;

        }

      }

      const a =
        document.createElement('a');

      a.href =
        URL.createObjectURL(
          await r.blob()
        );

      a.download = name;

      a.click();

      URL.revokeObjectURL(a.href);
    }

  };


  /* =========================================================
     HELPERS
     ========================================================= */

  const money = n =>
    new Intl.NumberFormat(
      'en-IN',
      {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 0
      }
    ).format(Number(n || 0));


  const date = d =>
    d
      ? new Date(d).toLocaleString(
          'en-IN',
          {
            dateStyle: 'medium',
            timeStyle: 'short'
          }
        )
      : '—';


  const escape = v =>
    String(v ?? '').replace(
      /[&<>'"]/g,
      c =>
        ({
          '&': '&amp;',
          '<': '&lt;',
          '>': '&gt;',
          "'": '&#39;',
          '"': '&quot;'
        }[c])
    );


  const options = (values, selected) =>
    values
      .map(
        v =>
          `<option
            ${v === selected ? 'selected' : ''}
            value="${v}">
            ${v}
          </option>`
      )
      .join('');


  // ---------------------------------------------------------
  // EYE ICON (password visibility toggle)
  // open = true  -> eye with a slash (password is visible)
  // open = false -> plain eye (password is hidden)
  // ---------------------------------------------------------
  function eyeIcon(open) {

    return open
      ? `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z"></path>
          <circle cx="12" cy="12" r="3"></circle>
          <line x1="3" y1="3" x2="21" y2="21"></line>
        </svg>`
      : `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z"></path>
          <circle cx="12" cy="12" r="3"></circle>
        </svg>`;

  }


  // ---------------------------------------------------------
  // Wires up every .pw-toggle button currently in the DOM.
  // Safe to call repeatedly (e.g. after re-rendering a page) —
  // it just (re)assigns onclick, no duplicate listeners pile up.
  // ---------------------------------------------------------
  function wirePasswordToggles(root = document) {

    root.querySelectorAll('.pw-toggle').forEach(btn => {

      btn.onclick = () => {

        const input = btn.previousElementSibling;

        if (!input) return;

        const showing = input.type === 'text';

        input.type = showing ? 'password' : 'text';

        btn.innerHTML = eyeIcon(!showing);

      };

    });

  }


  function toast(message, error = false) {

    const t = $('#toast');

    if (!t) return;

    t.textContent = message;

    t.className =
      `show ${error ? 'error' : ''}`;

    setTimeout(
      () => t.className = '',
      3800
    );

  }


  function formData(form) {

    return Object.fromEntries(
      new FormData(form).entries()
    );

  }


  function ensureDate(value) {

    const d =
      value
        ? new Date(value)
        : new Date();

    const pad =
      n => String(n).padStart(2, '0');

    return `${d.getFullYear()}-${pad(
      d.getMonth() + 1
    )}-${pad(
      d.getDate()
    )}T${pad(
      d.getHours()
    )}:${pad(
      d.getMinutes()
    )}`;

  }


  /* =========================================================
     THEME
     ========================================================= */

  function applyTheme(theme) {

    document.documentElement.dataset.theme =
      theme;

    localStorage.setItem(
      'nexspend.theme',
      theme
    );

    const button =
      $('#theme-toggle');

    if (button) {

      button.textContent =
        theme === 'dark'
          ? '☀'
          : '◐';

    }

  }


  function toggleTheme() {

    const current =
      document.documentElement.dataset.theme ||
      'light';

    applyTheme(
      current === 'dark'
        ? 'light'
        : 'dark'
    );

  }


  applyTheme(
    localStorage.getItem(
      'nexspend.theme'
    ) || 'light'
  );


  $('#theme-toggle')?.addEventListener(
    'click',
    toggleTheme
  );


  $('#dropdown-theme')?.addEventListener(
    'click',
    () => {

      toggleTheme();

      $('#profile-dropdown')
        ?.classList.add('hidden');

    }
  );


  /* =========================================================
     PROFILE UI
     ========================================================= */

  function initials(user) {

    if (!user) return 'A';

    const value =
      user.name ||
      user.email ||
      'A';

    return value
      .split(' ')
      .map(x => x[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();

  }


  function updateProfileUI() {

    const user =
      state.user || {};

    const name =
      user.name ||
      user.email?.split('@')[0] ||
      'Account';

    const email =
      user.email || '';


    const initial =
      initials(user);


    if ($('#profile-name'))
      $('#profile-name').textContent =
        name;

    if ($('#profile-email'))
      $('#profile-email').textContent =
        email;

    if ($('#dropdown-name'))
      $('#dropdown-name').textContent =
        name;

    if ($('#dropdown-email'))
      $('#dropdown-email').textContent =
        email;

    if ($('#profile-avatar'))
      $('#profile-avatar').textContent =
        initial;

    if ($('#dropdown-avatar'))
      $('#dropdown-avatar').textContent =
        initial;

  }


  function setupProfileMenu() {

    const trigger =
      $('#profile-trigger');

    const dropdown =
      $('#profile-dropdown');

    if (!trigger || !dropdown)
      return;


    trigger.onclick = e => {

      e.stopPropagation();

      dropdown.classList.toggle(
        'hidden'
      );

    };


    document.addEventListener(
      'click',
      () => {

        dropdown.classList.add(
          'hidden'
        );

      }
    );


    dropdown.addEventListener(
      'click',
      e => e.stopPropagation()
    );


    document
      .querySelectorAll(
        '[data-dropdown-page]'
      )
      .forEach(button => {

        button.onclick = () => {

          dropdown.classList.add(
            'hidden'
          );

          navigate(
            button.dataset.dropdownPage
          );

        };

      });


    $('#dropdown-logout')?.addEventListener(
      'click',
      () => {

        dropdown.classList.add(
          'hidden'
        );

        logout();

      }
    );

  }


  setupProfileMenu();


  /* =========================================================
     SESSION
     ========================================================= */

  function saveSession(result) {

    state.token =
      result.accessToken;

    state.refresh =
      result.refreshToken;

    state.user = {
      email: result.email,
      name:
        result.name ||
        result.email?.split('@')[0]
    };


    localStorage.setItem(
      'nexspend.accessToken',
      state.token
    );

    localStorage.setItem(
      'nexspend.refreshToken',
      state.refresh
    );

    localStorage.setItem(
      'nexspend.user',
      JSON.stringify(state.user)
    );


    showApp();

  }


  function showApp() {

    $('#auth-view')
      .classList.add('hidden');

    $('#app-view')
      .classList.remove('hidden');


    updateProfileUI();


    navigate(state.page);

  }


  function showAuth() {

    $('#app-view')
      .classList.add('hidden');

    $('#auth-view')
      .classList.remove('hidden');

  }


  /* =========================================================
     NAVIGATION
     ========================================================= */

  async function navigate(page) {

    state.page = page;


    document
      .querySelectorAll('#nav button')
      .forEach(
        b =>
          b.classList.toggle(
            'active',
            b.dataset.page === page
          )
      );


    $('#page-title').textContent =
      ({
        dashboard: 'Overview',
        expenses: 'Transactions',
        budgets: 'Budgets',
        recurring: 'Recurring payments',
        reports: 'Reports',
        profile: 'Account'
      })[page];


    $('#page-kicker').textContent =
      ({
        dashboard: 'YOUR MONEY AT A GLANCE',
        expenses: 'TRACK YOUR SPENDING',
        budgets: 'PLAN YOUR MONTH',
        recurring: 'AUTOMATE YOUR FINANCES',
        reports: 'UNDERSTAND YOUR FINANCES',
        profile: 'ACCOUNT & SECURITY'
      })[page];


    const render = {
      dashboard: renderDashboard,
      expenses: renderExpenses,
      budgets: renderBudgets,
      recurring: renderRecurring,
      reports: renderReports,
      profile: renderProfile
    }[page];


    try {

      await render();

    } catch (e) {

      $('#page').innerHTML =
        `<div class="panel">
          <h3>Something went wrong</h3>
          <p class="empty">
            ${escape(e.message)}
          </p>
        </div>`;

      toast(
        e.message,
        true
      );

    }

  }


  /* =========================================================
     OVERVIEW / DASHBOARD
     ========================================================= */

  async function renderDashboard() {

    const d =
      await api.dashboard();


    const balance =
      Number(d.balance || 0);

    const income =
      Number(d.totalIncome || 0);

    const expenses =
      Number(d.totalExpense || 0);

    const monthly =
      Number(d.monthlyExpense || 0);

    const savings =
      Number(d.savingsRate || 0);

    const utilization =
      Number(
        d.budgetUtilizationPercentage ||
        d.budgetUtilization ||
        0
      );


    const trends =
      d.expenseTrends ||
      d.monthlyExpenseTrend ||
      d.expenseTrend ||
      {};


    const trendEntries =
      Object.entries(trends)
        .filter(
          ([, value]) =>
            Number(value || 0) >= 0
        );


    const maxTrend =
      Math.max(
        ...trendEntries.map(
          ([, value]) =>
            Number(value || 0)
        ),
        1
      );


    let categoryData = null;


    const possibleCategoryData = [
      d.expenseByCategory,
      d.categoryExpenses,
      d.categoryWiseExpenses,
      d.categoryWiseExpense,
      d.expensesByCategory,
      d.expensesByCategories,
      d.categorySpending
    ];


    for (const value of possibleCategoryData) {

      if (
        value &&
        typeof value === 'object' &&
        !Array.isArray(value) &&
        Object.keys(value).length > 0
      ) {

        categoryData = value;

        break;

      }

    }


    if (!categoryData) {

      try {

        const transactionResult =
          await api.expenses({
            page: 0,
            size: 1000,
            sort: 'date,desc'
          });


        const transactions =
          transactionResult?.content ||
          transactionResult ||
          [];


        categoryData = {};


        transactions
          .filter(
            transaction =>
              String(
                transaction.type || ''
              ).toUpperCase() === 'EXPENSE'
          )
          .forEach(
            transaction => {

              const category =
                String(
                  transaction.category ||
                  'OTHER'
                )
                .trim()
                .toUpperCase();


              const amount =
                Number(
                  transaction.amount || 0
                );


              if (amount > 0) {

                categoryData[category] =
                  (
                    categoryData[category] || 0
                  ) + amount;

              }

            }
          );

      } catch (error) {

        console.error(
          'Unable to calculate category spending:',
          error
        );

        categoryData = {};

      }

    }


    const categoryEntries =
      Object.entries(categoryData || {})
        .map(
          ([category, value]) => [
            String(category).toUpperCase(),
            Number(value || 0)
          ]
        )
        .filter(
          ([, value]) =>
            value > 0
        )
        .sort(
          (a, b) =>
            b[1] - a[1]
        );


    const totalCategoryExpense =
      categoryEntries.reduce(
        (sum, [, value]) =>
          sum + value,
        0
      );


    const calculatedTopCategory =
      categoryEntries.length
        ? categoryEntries[0][0]
        : null;


    const topCategory =
      d.topCategory ||
      d.mostSpentCategory ||
      d.highestExpenseCategory ||
      calculatedTopCategory ||
      'N/A';


    const budgetSpent =
      Number(
        d.budgetSpent ||
        d.totalBudgetSpent ||
        0
      );


    const budgetAmount =
      Number(
        d.totalBudget ||
        d.budgetAmount ||
        0
      );


    const recentTransactions =
      d.last10Transactions ||
      d.recentTransactions ||
      d.transactions ||
      [];


    $('#page').innerHTML = `

      <div class="grid stats">

        <article class="panel stat">
          <p>Total balance</p>
          <strong>${money(balance)}</strong>
          <div class="stat-sub">Available balance</div>
        </article>

        <article class="panel stat">
          <p>Total income</p>
          <strong class="amount-income">${money(income)}</strong>
          <div class="stat-sub">Money received</div>
        </article>

        <article class="panel stat">
          <p>Total expenses</p>
          <strong class="amount-expense">${money(expenses)}</strong>
          <div class="stat-sub">Overall spending</div>
        </article>

        <article class="panel stat">
          <p>Monthly spending</p>
          <strong>${money(monthly)}</strong>
          <div class="stat-sub">${savings.toFixed(1)}% savings rate</div>
        </article>

      </div>


      <div class="grid dashboard-main">

        <article class="panel analytics-panel">

          <div class="panel-header">
            <div>
              <h3>Monthly expense trend</h3>
              <p class="panel-subtitle">Your spending activity over time</p>
            </div>
            <span class="auth-badge">MONTHLY</span>
          </div>

          ${
            trendEntries.length
              ? `
                <div class="chart">
                  ${trendEntries.map(([label, value]) => {
                    const amount = Number(value || 0);
                    const height = Math.max((amount / maxTrend) * 100, 3);
                    return `
                      <div class="chart-column">
                        <span class="chart-value">${money(amount)}</span>
                        <div class="chart-bar" style="height:${height}%;" title="${money(amount)}"></div>
                        <span class="chart-label">${escape(label)}</span>
                      </div>
                    `;
                  }).join('')}
                </div>
                <div class="analytics-legend">
                  <span><i class="legend-dot"></i>Expenses</span>
                  <span>${money(monthly)} this month</span>
                </div>
              `
              : `<p class="empty">No expense trend data available yet.</p>`
          }

        </article>


        <article class="panel">

          <div class="panel-header">
            <div>
              <h3>Budget performance</h3>
              <p class="panel-subtitle">Current budget utilisation</p>
            </div>
          </div>

          <div class="budget-summary">

            <div class="budget-total">
              <div>
                <small>UTILISATION</small>
                <strong>${utilization.toFixed(1)}%</strong>
              </div>
              <span>${utilization > 100 ? 'Over budget' : 'On track'}</span>
            </div>

            <div class="budget-progress">
              <span style="width:${Math.min(utilization, 100)}%;"></span>
            </div>

            <div class="budget-meta">
              <span>${budgetSpent ? money(budgetSpent) : 'Current spending'}</span>
              <span>${budgetAmount ? money(budgetAmount) : 'Budget'}</span>
            </div>

            <div class="metric-list">
              <div class="metric-row">
                <span>Monthly expense</span>
                <b>${money(monthly)}</b>
              </div>
              <div class="metric-row">
                <span>Savings rate</span>
                <b>${savings.toFixed(1)}%</b>
              </div>
              <div class="metric-row">
                <span>Top category</span>
                <b>${escape(topCategory)}</b>
              </div>
            </div>

          </div>

        </article>

      </div>


      <div class="grid dashboard-bottom">

        <article class="panel">

          <div class="panel-header">
            <div>
              <h3>Category spending</h3>
              <p class="panel-subtitle">Where your money is going</p>
            </div>
          </div>

          ${
            categoryEntries.length
              ? `
                <div class="category-list">
                  ${categoryEntries.slice(0, 6).map(([category, value]) => {
                    const percentage = totalCategoryExpense > 0
                      ? (value / totalCategoryExpense) * 100
                      : 0;
                    return `
                      <div class="category-row">
                        <div class="category-info">
                          <span>${escape(category)}</span>
                          <strong>${money(value)}</strong>
                        </div>
                        <div class="progress">
                          <span style="width:${Math.min(percentage, 100)}%;" title="${percentage.toFixed(1)}%"></span>
                        </div>
                        <small style="display:block;margin-top:4px;color:var(--muted);">
                          ${percentage.toFixed(1)}%
                        </small>
                      </div>
                    `;
                  }).join('')}
                </div>
              `
              : `
                <div class="category-list">
                  <div class="category-row">
                    <div class="category-info">
                      <span>Top category</span>
                      <strong>N/A</strong>
                    </div>
                    <div class="progress"><span style="width:0%"></span></div>
                  </div>
                  <p class="empty">Add expense transactions to see category-wise spending here.</p>
                </div>
              `
          }

        </article>


        <article class="panel">

          <div class="panel-header">
            <div>
              <h3>Recent transactions</h3>
              <p class="panel-subtitle">Your latest financial activity</p>
            </div>
          </div>

          ${transactions(recentTransactions)}

        </article>

      </div>


      <div class="grid dashboard-bottom" style="margin-top:16px">

        <article class="panel">

          <div class="panel-header">
            <div>
              <h3>Upcoming recurring payments</h3>
              <p class="panel-subtitle">Keep track of scheduled expenses</p>
            </div>
          </div>

          ${
            (d.upcomingRecurringExpenses || [])
              .map(x => `
                <div class="metric-row">
                  <span>${escape(x.description)} <small>(${escape(x.frequency)})</small></span>
                  <b>${money(x.amount)}</b>
                </div>
              `)
              .join('')
            || '<p class="empty">No recurring payments.</p>'
          }

        </article>


        <article class="panel">

          <div class="panel-header">
            <div>
              <h3>Financial snapshot</h3>
              <p class="panel-subtitle">Key indicators for your finances</p>
            </div>
          </div>

          <div class="metric-list">
            <div class="metric-row">
              <span>Today's expenses</span>
              <b>${money(d.todayExpense)}</b>
            </div>
            <div class="metric-row">
              <span>Top category</span>
              <b>${escape(topCategory)}</b>
            </div>
            <div class="metric-row">
              <span>Savings rate</span>
              <b>${savings.toFixed(1)}%</b>
            </div>
            <div class="metric-row">
              <span>Budget utilisation</span>
              <b>${utilization.toFixed(1)}%</b>
            </div>
          </div>

        </article>

      </div>

    `;
  }


  /* =========================================================
     TRANSACTIONS
     ========================================================= */

  function transactions(items) {

    return items.length
      ? `
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Category</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody>
              ${items.map(x => `
                <tr>
                  <td>${date(x.date)}</td>
                  <td>${escape(x.description)}</td>
                  <td>${escape(x.category)}</td>
                  <td class="amount-${String(x.type).toLowerCase()}">
                    ${x.type === 'INCOME' ? '+' : '-'}${money(x.amount)}
                  </td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      `
      : '<p class="empty">No transactions yet.</p>';

  }


  /* =========================================================
     EXPENSES
     ========================================================= */

  async function renderExpenses() {

    const result =
      await api.expenses({
        page: state.expensePage,
        size: 10,
        sort: 'date,desc'
      });


    const items =
      result.content || [];


    $('#page').innerHTML = `

      <section class="panel">
        <form id="expense-form" class="toolbar">

          <input type="hidden" name="id">

          <label>
            Description
            <input name="description" required>
          </label>

          <label>
            Amount
            <input name="amount" type="number" min="0.01" step="0.01" required>
          </label>

          <label>
            Category
            <select name="category">${options(CATEGORIES)}</select>
          </label>

          <label>
            Type
            <select name="type">${options(TYPES, 'EXPENSE')}</select>
          </label>

          <label>
            Date
            <input name="date" type="datetime-local" value="${ensureDate()}">
          </label>

          <button class="primary">Save transaction</button>
          <button type="button" id="expense-reset">Clear</button>

        </form>
      </section>


      <section class="panel" style="margin-top:16px">

        <form id="expense-filter" class="toolbar">

          <label>
            Category
            <select name="category">
              <option value="">All</option>
              ${options(CATEGORIES)}
            </select>
          </label>

          <label>
            Type
            <select name="type">
              <option value="">All</option>
              ${options(TYPES)}
            </select>
          </label>

          <label>
            From
            <input type="datetime-local" name="startDate">
          </label>

          <label>
            To
            <input type="datetime-local" name="endDate">
          </label>

          <button class="primary">Filter</button>
          <button type="button" id="clear-filter">Reset</button>

        </form>

        <div id="expense-list"></div>

        <div class="pagination">
          <button id="prev-expense" ${result.first ? 'disabled' : ''}>Previous</button>
          <span>Page ${(result.number || 0) + 1} of ${Math.max(result.totalPages || 1, 1)}</span>
          <button id="next-expense" ${result.last ? 'disabled' : ''}>Next</button>
        </div>

      </section>

    `;


    $('#expense-list').innerHTML =
      items.length
        ? expenseTable(items)
        : '<p class="empty">No transactions yet.</p>';


    $('#expense-form').onsubmit = saveExpense;

    $('#expense-reset').onclick =
      () => { $('#expense-form').reset(); };

    $('#expense-filter').onsubmit = filterExpenses;

    $('#clear-filter').onclick =
      () => renderExpenses();

    $('#prev-expense').onclick = () => {
      state.expensePage--;
      renderExpenses();
    };

    $('#next-expense').onclick = () => {
      state.expensePage++;
      renderExpenses();
    };


    document.querySelectorAll('[data-edit-expense]')
      .forEach(b => b.onclick = () => editExpense(b.dataset.editExpense));

    document.querySelectorAll('[data-delete-expense]')
      .forEach(b => b.onclick = () => deleteExpense(b.dataset.deleteExpense));

  }


  function expenseTable(items) {

    return `
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Description</th>
              <th>Category</th>
              <th>Type</th>
              <th>Amount</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            ${items.map(x => `
              <tr>
                <td>${date(x.date)}</td>
                <td>${escape(x.description)}</td>
                <td>${escape(x.category)}</td>
                <td>${escape(x.type)}</td>
                <td class="amount-${String(x.type).toLowerCase()}">${money(x.amount)}</td>
                <td>
                  <button data-edit-expense="${x.id}">Edit</button>
                  <button class="danger" data-delete-expense="${x.id}">Delete</button>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `;

  }


  async function saveExpense(e) {

    e.preventDefault();

    const f = e.currentTarget;
    const d = formData(f);
    const id = d.id;

    delete d.id;

    try {

      await (id ? api.updateExpense(id, d) : api.createExpense(d));

      toast(id ? 'Transaction updated' : 'Transaction created');

      state.expensePage = 0;

      renderExpenses();

    } catch (err) {

      toast(err.message, true);

    }

  }


  async function editExpense(id) {

    try {

      const x = await api.expense(id);
      const f = $('#expense-form');

      Object.entries(x).forEach(([k, v]) => {
        if (f.elements[k]) {
          f.elements[k].value = k === 'date' ? ensureDate(v) : v;
        }
      });

      f.scrollIntoView({ behavior: 'smooth', block: 'start' });
      f.elements.description?.focus();

    } catch (e) {

      toast(e.message, true);

    }

  }


  async function deleteExpense(id) {

    if (confirm('Delete this transaction?')) {

      try {

        await api.deleteExpense(id);
        toast('Transaction deleted');
        renderExpenses();

      } catch (e) {

        toast(e.message, true);

      }

    }

  }


  async function filterExpenses(e) {

    e.preventDefault();

    const q = formData(e.currentTarget);

    Object.keys(q).forEach(k => { if (!q[k]) delete q[k]; });

    try {

      const r = await api.filterExpenses(q);

      $('#expense-list').innerHTML = expenseTable(r.content || []);

      document.querySelectorAll('[data-edit-expense]')
        .forEach(b => b.onclick = () => editExpense(b.dataset.editExpense));

      document.querySelectorAll('[data-delete-expense]')
        .forEach(b => b.onclick = () => deleteExpense(b.dataset.deleteExpense));

      $('.pagination').classList.add('hidden');

    } catch (err) {

      toast(err.message, true);

    }

  }


  /* =========================================================
     BUDGETS
     ========================================================= */

  async function renderBudgets() {

    const items = await api.budgets();
    const now = new Date();


    $('#page').innerHTML = `

      <section class="panel">
        <form id="budget-form" class="toolbar">

          <label>
            Category
            <select name="category">${options(CATEGORIES)}</select>
          </label>

          <label>
            Monthly limit
            <input name="amount" type="number" min="0.01" step="0.01" required>
          </label>

          <label>
            Month
            <input name="month" type="number" min="1" max="12" value="${now.getMonth() + 1}" required>
          </label>

          <label>
            Year
            <input name="year" type="number" value="${now.getFullYear()}" required>
          </label>

          <button class="primary">Save budget</button>

        </form>
      </section>


      <section class="panel" style="margin-top:16px">

        <h3>All budgets</h3>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Period</th>
                <th>Category</th>
                <th>Budget</th>
                <th>Spent</th>
                <th>Remaining</th>
              </tr>
            </thead>
            <tbody>
              ${items.map(x => `
                <tr>
                  <td>${x.month}/${x.year}</td>
                  <td>${escape(x.category)}</td>
                  <td>${money(x.amount)}</td>
                  <td>${money(x.spent)}</td>
                  <td>${money(x.remaining)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>

      </section>

    `;


    $('#budget-form').onsubmit = async e => {

      e.preventDefault();

      try {

        await api.saveBudget(formData(e.currentTarget));
        toast('Budget saved');
        renderBudgets();

      } catch (err) {

        toast(err.message, true);

      }

    };

  }


  /* =========================================================
     RECURRING
     ========================================================= */

  async function renderRecurring() {

    const items = await api.recurring();


    $('#page').innerHTML = `

      <section class="panel">
        <form id="recurring-form" class="toolbar">

          <label>
            Description
            <input name="description" required>
          </label>

          <label>
            Amount
            <input name="amount" type="number" min="0.01" step="0.01" required>
          </label>

          <label>
            Category
            <select name="category">${options(CATEGORIES)}</select>
          </label>

          <label>
            Type
            <select name="type">${options(TYPES, 'EXPENSE')}</select>
          </label>

          <label>
            Frequency
            <select name="frequency">
              ${options(['WEEKLY', 'MONTHLY', 'YEARLY'], 'MONTHLY')}
            </select>
          </label>

          <label>
            First date
            <input name="startDate" type="datetime-local" value="${ensureDate()}" required>
          </label>

          <button class="primary">Add recurring</button>

        </form>
      </section>


      <section class="panel" style="margin-top:16px">

        <h3>Scheduled payments</h3>

        ${
          items.length
            ? `
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Description</th>
                      <th>Frequency</th>
                      <th>Next due</th>
                      <th>Amount</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    ${items.map(x => `
                      <tr>
                        <td>${escape(x.description)}</td>
                        <td>${escape(x.frequency)}</td>
                        <td>${date(x.nextExecutionDate)}</td>
                        <td>${money(x.amount)}</td>
                        <td>${x.active ? 'Active' : 'Inactive'}</td>
                      </tr>
                    `).join('')}
                  </tbody>
                </table>
              </div>
            `
            : `<p class="empty">No recurring expenses.</p>`
        }

      </section>

    `;


    $('#recurring-form').onsubmit = async e => {

      e.preventDefault();

      try {

        await api.createRecurring(formData(e.currentTarget));
        toast('Recurring expense added');
        renderRecurring();

      } catch (err) {

        toast(err.message, true);

      }

    };

  }


  /* =========================================================
     REPORTS
     ========================================================= */

  async function renderReports() {

    $('#page').innerHTML = `

      <section class="reports-hero">

        <div class="reports-hero-content">
          <span class="reports-badge">FINANCIAL REPORTS</span>
          <h1>Understand your <span>financial journey.</span></h1>
          <p>Export your NexSpend financial data into clean, professional reports whenever you need them.</p>
        </div>

        <div class="reports-hero-icon"><span>▥</span></div>

      </section>


      <div class="reports-grid">

        <article class="panel report-card enhanced-report-card">

          <div class="report-card-top">
            <div class="report-icon excel-icon">▦</div>
            <span class="report-format">XLSX</span>
          </div>

          <div class="report-card-content">
            <h3>Monthly Excel report</h3>
            <p>Download an editable workbook containing your transactions, budgets, income, expenses and other financial data.</p>
            <div class="report-features">
              <span>✓ Transactions</span>
              <span>✓ Budgets</span>
              <span>✓ Financial data</span>
            </div>
          </div>

          <button class="report-download primary" id="excel"><span>↓</span> Download Excel</button>

        </article>


        <article class="panel report-card enhanced-report-card">

          <div class="report-card-top">
            <div class="report-icon pdf-icon">▥</div>
            <span class="report-format">PDF</span>
          </div>

          <div class="report-card-content">
            <h3>Monthly PDF report</h3>
            <p>Download a clean and professional summary of your monthly financial activity for storing or sharing.</p>
            <div class="report-features">
              <span>✓ Monthly summary</span>
              <span>✓ Easy to share</span>
              <span>✓ Print ready</span>
            </div>
          </div>

          <button class="report-download primary" id="pdf"><span>↓</span> Download PDF</button>

        </article>

      </div>


      <section class="report-tip">
        <span class="report-tip-icon">💡</span>
        <div>
          <strong>Keep your finances organized</strong>
          <p>Use Excel when you want to analyse or edit your financial data. Use PDF when you need a clean snapshot of your monthly finances.</p>
        </div>
      </section>

    `;


    $('#excel').onclick = () =>
      api.download('/api/report/excel', 'nexspend-report.xlsx')
        .catch(e => toast(e.message, true));

    $('#pdf').onclick = () =>
      api.download('/api/report/pdf', 'nexspend-report.pdf')
        .catch(e => toast(e.message, true));

  }


  /* =========================================================
     PROFILE / ACCOUNT
     ========================================================= */

  async function renderProfile() {

    const u = await api.me();

    const initial =
      (u.name || 'A').trim().charAt(0).toUpperCase();


    $('#page').innerHTML = `

      <section class="account-hero">

        <div class="account-profile">

          <div class="account-avatar">${escape(initial)}</div>

          <div class="account-profile-info">
            <span class="account-label">ACCOUNT PROFILE</span>
            <h1>${escape(u.name)}</h1>
            <p>${escape(u.email)}</p>
          </div>

        </div>

        <div class="account-status">
          <span class="account-status-dot"></span>
          Account active
        </div>

      </section>


      <!-- PERSONAL INFO FORM -->

      <form id="profile-form" class="account-settings-form">

        <input name="id" type="hidden" value="${u.id}">

        <section class="panel account-card">

          <div class="account-card-header">
            <div class="account-card-icon">○</div>
            <div>
              <span class="account-section-label">PERSONAL INFORMATION</span>
              <h2>Profile details</h2>
              <p>Update the information associated with your NexSpend account.</p>
            </div>
          </div>

          <div class="account-fields">

            <label class="account-field">
              <span>Full name</span>
              <input name="name" value="${escape(u.name)}" autocomplete="name" required />
              <small>This name will be displayed throughout NexSpend.</small>
            </label>

            <label class="account-field">
              <span>Email address</span>
              <input name="email" type="email" value="${escape(u.email)}" autocomplete="email" required />
              <small>Your registered email address.</small>
            </label>

          </div>

        </section>


        <div class="account-save-bar">
          <div>
            <strong>Ready to save your changes?</strong>
            <span>Your account information will be updated immediately.</span>
          </div>
          <button class="primary account-save-button" type="submit">
            <span>✓</span> Save changes
          </button>
        </div>

      </form>


      <!-- =================================================
           CHANGE PASSWORD FORM
           Separate form, own endpoint, requires the correct
           current password before the new one is saved.
           ================================================= -->

      <section class="panel account-card" style="margin-top:18px">

        <div class="account-card-header">
          <div class="account-card-icon security-icon">◈</div>
          <div>
            <span class="account-section-label">SECURITY</span>
            <h2>Password</h2>
            <p>Set a new password to keep your account secure.</p>
          </div>
        </div>

        <form id="password-form" class="security-content">

          <input name="id" type="hidden" value="${u.id}">

          <div class="security-message">
            <span class="security-message-icon">✓</span>
            <div>
              <strong>Protect your account</strong>
              <p>Use a password with at least 6 characters and avoid using information that is easy to guess.</p>
            </div>
          </div>

          <label class="account-field">
            <span>Current password</span>
            <div class="input-wrapper">
              <input name="oldPassword" type="password" autocomplete="current-password" required>
              <button type="button" class="pw-toggle" data-target="oldPassword" tabindex="-1">${eyeIcon(false)}</button>
            </div>
          </label>

          <label class="account-field">
            <span>New password</span>
            <div class="input-wrapper">
              <input name="newPassword" type="password" minlength="6" autocomplete="new-password" required>
              <button type="button" class="pw-toggle" data-target="newPassword" tabindex="-1">${eyeIcon(false)}</button>
            </div>
            <small>Minimum 6 characters.</small>
          </label>

          <label class="account-field">
            <span>Confirm new password</span>
            <div class="input-wrapper">
              <input name="confirmPassword" type="password" minlength="6" autocomplete="new-password" required>
              <button type="button" class="pw-toggle" data-target="confirmPassword" tabindex="-1">${eyeIcon(false)}</button>
            </div>
          </label>

          <button class="primary account-save-button" type="submit">
            <span>✓</span> Change password
          </button>

        </form>

      </section>


      <section class="panel account-danger">

        <div class="danger-info">
          <div class="danger-icon">!</div>
          <div>
            <span class="account-section-label danger-label">DANGER ZONE</span>
            <h2>Delete account</h2>
            <p>Permanently delete your NexSpend account and associated data. This action cannot be undone.</p>
          </div>
        </div>

        <button class="danger account-delete-button" type="button" id="delete-profile">
          Delete account
        </button>

      </section>

    `;


    $('#profile-form').onsubmit = saveProfile;
    $('#password-form').onsubmit = savePassword;

    $('#delete-profile').onclick = () => deleteProfile(u.id);

    // Show/hide toggles for the three password fields.
    wirePasswordToggles();

  }


  async function saveProfile(e) {

    e.preventDefault();

    const d = formData(e.currentTarget);

    try {

      // Password is no longer sent from this form —
      // it has its own dedicated endpoint/form below.
      await api.updateUser(d.id, {
        name: d.name,
        email: d.email
      });

      toast('Profile updated');

      state.user = {
        ...state.user,
        name: d.name,
        email: d.email
      };

      localStorage.setItem('nexspend.user', JSON.stringify(state.user));

      updateProfileUI();

    } catch (err) {

      toast(err.message, true);

    }

  }


  // ---------------------------------------------------------
  // CHANGE PASSWORD
  // Old password must be correct before the new one saves.
  // Uses existing /api/users/{id}/change-password endpoint.
  // ---------------------------------------------------------
  async function savePassword(e) {

    e.preventDefault();

    const form = e.currentTarget;
    const d = formData(form);

    if (d.newPassword !== d.confirmPassword) {

      toast("New password and confirmation don't match.", true);

      return;

    }

    if (d.newPassword.length < 6) {

      toast('New password must be at least 6 characters.', true);

      return;

    }

    try {

      await api.changePassword(d.id, {
        oldPassword: d.oldPassword,
        newPassword: d.newPassword,
        confirmPassword: d.confirmPassword
      });

      toast('Password changed successfully.');

      form.reset();

    } catch (err) {

      toast(err.message || 'Unable to change password.', true);

    }

  }


  async function deleteProfile(id) {

    if (confirm('Delete this account?')) {

      try {

        await api.deleteUser(id);
        logout();

      } catch (e) {

        toast(e.message, true);

      }

    }

  }


  /* =========================================================
     LOGOUT
     ========================================================= */

  function logout() {

    state.token = null;
    state.refresh = null;
    state.user = null;

    localStorage.removeItem('nexspend.accessToken');
    localStorage.removeItem('nexspend.refreshToken');
    localStorage.removeItem('nexspend.user');

    showAuth();

  }


  /* =========================================================
     AUTH TABS
     ========================================================= */

  document.querySelectorAll('[data-auth]').forEach(b => {

    b.onclick = () => {

      document.querySelectorAll('[data-auth]')
        .forEach(x => x.classList.toggle('active', x === b));

      $('#login-form').classList.toggle('hidden', b.dataset.auth !== 'login');
      $('#register-form').classList.toggle('hidden', b.dataset.auth !== 'register');

    };

  });


  /* =========================================================
     LOGIN
     ========================================================= */

  $('#login-form').onsubmit = async e => {

    e.preventDefault();

    try {

      saveSession(await api.login(formData(e.currentTarget)));
      toast('Welcome back');

    } catch (err) {

      toast(err.message, true);

    }

  };


  /* =========================================================
     REGISTER
     ========================================================= */

  $('#register-form').onsubmit = async e => {

    e.preventDefault();

    try {

      await api.register(formData(e.currentTarget));
      toast('Account created. Check your email to activate it.');
      document.querySelector('[data-auth="login"]').click();

    } catch (err) {

      toast(err.message, true);

    }

  };


  // Wire up the eye-icon toggles on the login and register
  // password fields. These forms are static HTML present from
  // page load (unlike the profile page, which is re-rendered
  // in JS), so they only need to be wired once here.
  wirePasswordToggles($('#auth-view'));


  /* =========================================================
     NAVIGATION EVENTS
     ========================================================= */

  $('#nav').onclick = e => {

    const b = e.target.closest('[data-page]');

    if (b) {

      navigate(b.dataset.page);

    }

  };


  $('#logout')?.addEventListener('click', logout);


  /* =========================================================
     ACTIVATION RESULT
     =========================================================

     The backend handles the actual activation.

     Successful activation redirects to:  /?activated=true
     Failed/expired activation redirects to: /?activationError=...

     The user is NEVER automatically logged in.
     ========================================================= */

  const urlParams = new URLSearchParams(window.location.search);

  const activationSuccess = urlParams.get('activated');
  const activationError = urlParams.get('activationError');


  if (activationSuccess === 'true') {

    state.token = null;
    state.refresh = null;
    state.user = null;

    localStorage.removeItem('nexspend.accessToken');
    localStorage.removeItem('nexspend.refreshToken');
    localStorage.removeItem('nexspend.user');

    showAuth();

    document.querySelector('[data-auth="login"]')?.click();

    window.history.replaceState({}, document.title, window.location.pathname);

    toast('Account activated successfully. You can now sign in.');

  } else if (activationError) {

    state.token = null;
    state.refresh = null;
    state.user = null;

    localStorage.removeItem('nexspend.accessToken');
    localStorage.removeItem('nexspend.refreshToken');
    localStorage.removeItem('nexspend.user');

    showAuth();

    document.querySelector('[data-auth="login"]')?.click();

    const message = activationError;

    window.history.replaceState({}, document.title, window.location.pathname);

    toast(message, true);

  }


  /* =========================================================
     INITIAL STATE
     ========================================================= */

  if (!activationSuccess && !activationError) {

    if (state.token) {

      updateProfileUI();
      showApp();

    } else {

      showAuth();

    }

  }

})();